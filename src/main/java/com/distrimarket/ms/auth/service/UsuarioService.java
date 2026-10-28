package com.distrimarket.ms.auth.service;

import com.distrimarket.commons.dto.UsuarioCreateRequestDTO;
import com.distrimarket.commons.dto.UsuarioResponseDTO;
import com.distrimarket.commons.dto.UsuarioUpdateRequestDTO;
import com.distrimarket.commons.entity.Empleado;
import com.distrimarket.commons.entity.Persona;
import com.distrimarket.commons.entity.Rol;
import com.distrimarket.commons.entity.Usuario;
import com.distrimarket.commons.enums.RolUsuario;
import com.distrimarket.ms.auth.config.CacheConfig;
import com.distrimarket.ms.auth.config.DomainCache;
import com.distrimarket.ms.auth.exception.ConflictException;
import com.distrimarket.ms.auth.exception.ResourceNotFoundException;
import com.distrimarket.ms.auth.mapper.UsuarioMapper;
import com.distrimarket.ms.auth.repository.EmpleadoRepository;
import com.distrimarket.ms.auth.repository.PersonaRepository;
import com.distrimarket.ms.auth.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.JoinType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Administración de usuarios. Cada operación pública mantiene la caché del dominio "usuario":
 * lectura por id con {@code @Cacheable}, alta y cambios con {@code @CachePut}, baja con
 * {@code @CacheEvict}; el listado paginado no se cachea, pero sí cada usuario que trae.
 */
@Slf4j
@Service
public class UsuarioService {

    private static final Set<String> ORDENES_PERMITIDOS = Set.of("id", "username", "fechaCreacion");

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final RolService rolService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;
    private final DomainCache domainCache;
    private final EntityManager entityManager;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PersonaRepository personaRepository,
                          EmpleadoRepository empleadoRepository,
                          RolService rolService,
                          PasswordEncoder passwordEncoder,
                          UsuarioMapper mapper,
                          DomainCache domainCache,
                          EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.empleadoRepository = empleadoRepository;
        this.rolService = rolService;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.domainCache = domainCache;
        this.entityManager = entityManager;
    }

    @Cacheable(cacheNames = CacheConfig.USUARIO, key = "#id")
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtener(Long id) {
        validarId(id);
        log.debug("Buscando usuario {} en la base", id);
        return toResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> listar(String query, Pageable pageable) {
        Page<Usuario> usuarios = usuarioRepository.findAll(busqueda(query), conOrdenPermitido(pageable));
        // Una sola consulta para todos los empleados de la página, en vez de una por usuario.
        Map<Long, Empleado> empleados = empleadosPorPersona(usuarios.getContent());
        Page<UsuarioResponseDTO> pagina = usuarios.map(usuario -> toResponse(usuario, empleados.get(personaId(usuario))));
        // La página no se cachea (depende de filtros y orden); cada usuario sí, por su id.
        domainCache.putAll(CacheConfig.USUARIO, pagina.getContent(), UsuarioResponseDTO::getIdUsuario);
        return pagina;
    }

    @CachePut(cacheNames = CacheConfig.USUARIO, key = "#result.idUsuario")
    @Transactional
    public UsuarioResponseDTO crear(UsuarioCreateRequestDTO request) {
        String username = normalizarUsername(request.getUsername());
        if (usuarioRepository.existsUsername(username)) {
            throw new ConflictException("Ya existe un usuario con el nombre '" + username + "'.");
        }
        Persona persona = resolverPersona(request.getIdPersona(), request.getIdEmpleado());
        if (usuarioRepository.existsByPersonaIncluyendoInactivos(persona.getId())) {
            throw new ConflictException("La persona " + persona.getNombreCompleto()
                    + " ya tiene un usuario (activo o dado de baja).");
        }
        Usuario usuario = Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .persona(persona)
                .roles(roles(request.getRoles()))
                .activo(true)
                .build();
        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario '{}' creado (id {}) con roles {}", username, guardado.getId(), request.getRoles());
        return toResponse(guardado);
    }

    @CachePut(cacheNames = CacheConfig.USUARIO, key = "#id")
    @Transactional
    public UsuarioResponseDTO actualizar(Long id, UsuarioUpdateRequestDTO request) {
        validarId(id);
        Usuario usuario = buscar(id);
        String username = normalizarUsername(request.getUsername());
        if (usuarioRepository.existsUsernameAndIdNot(username, id)) {
            throw new ConflictException("Ya existe un usuario con el nombre '" + username + "'.");
        }
        if (tieneRol(usuario, RolUsuario.ADMIN) && !request.getRoles().contains(RolUsuario.ADMIN)) {
            validarQueQuedeOtroAdministrador("No se puede quitar el rol ADMIN al último administrador activo.");
        }
        usuario.setUsername(username);
        usuario.getRoles().clear();
        usuario.getRoles().addAll(roles(request.getRoles()));
        boolean cambiaPassword = request.getPassword() != null && !request.getPassword().isBlank();
        if (cambiaPassword) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        log.info("Usuario {} actualizado: username='{}' roles={}{}", id, username, request.getRoles(),
                cambiaPassword ? " (contraseña cambiada)" : "");
        return toResponse(usuarioRepository.save(usuario));
    }

    @CacheEvict(cacheNames = CacheConfig.USUARIO, key = "#id")
    @Transactional
    public void eliminar(Long id, String usuarioActual) {
        validarId(id);
        Usuario usuario = buscar(id);
        if (usuario.getUsername().equalsIgnoreCase(usuarioActual)) {
            throw new ConflictException("No puede dar de baja su propio usuario.");
        }
        if (tieneRol(usuario, RolUsuario.ADMIN)) {
            validarQueQuedeOtroAdministrador("No se puede dar de baja al último administrador activo.");
        }
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuario {} ('{}') dado de baja por {}", id, usuario.getUsername(), usuarioActual);
    }

    /** Empleado activo de la persona, para incluir su id en el token. */
    @Transactional(readOnly = true)
    public Long idEmpleadoDePersona(Long idPersona) {
        return empleadoDePersona(idPersona).map(Empleado::getId).orElse(null);
    }

    /** Empleado de la persona con su depósito (una sola consulta), para el token. */
    @Transactional(readOnly = true)
    public java.util.Optional<Empleado> empleadoDePersona(Long idPersona) {
        return idPersona == null ? java.util.Optional.empty() : empleadoRepository.findFirstByPersona_Id(idPersona);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    private UsuarioResponseDTO toResponse(Usuario usuario) {
        return toResponse(usuario, usuario.getPersona() == null ? null
                : empleadoRepository.findFirstByPersona_Id(usuario.getPersona().getId()).orElse(null));
    }

    private UsuarioResponseDTO toResponse(Usuario usuario, Empleado empleado) {
        return mapper.toResponse(usuario, empleado);
    }

    private Map<Long, Empleado> empleadosPorPersona(Collection<Usuario> usuarios) {
        Set<Long> idsPersona = usuarios.stream()
                .map(UsuarioService::personaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (idsPersona.isEmpty()) {
            return Map.of();
        }
        return empleadoRepository.findByPersona_IdIn(idsPersona).stream()
                .collect(Collectors.toMap(empleado -> empleado.getPersona().getId(),
                        Function.identity(), (primero, segundo) -> primero));
    }

    private static Long personaId(Usuario usuario) {
        return usuario.getPersona() == null ? null : usuario.getPersona().getId();
    }

    /** El usuario se asocia a una persona; si se indica un empleado, se usa su persona. */
    private Persona resolverPersona(Long idPersona, Long idEmpleado) {
        if (idEmpleado != null) {
            Empleado empleado = empleadoRepository.findWithPersonaById(idEmpleado)
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado", idEmpleado));
            Persona persona = empleado.getPersona();
            if (idPersona != null && !idPersona.equals(persona.getId())) {
                throw new IllegalArgumentException("El empleado " + idEmpleado
                        + " no corresponde a la persona " + idPersona + ".");
            }
            return persona;
        }
        if (idPersona == null) {
            throw new IllegalArgumentException("Debe indicar idPersona o idEmpleado.");
        }
        return personaRepository.findById(idPersona)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", idPersona));
    }

    private Set<Rol> roles(Collection<RolUsuario> nombres) {
        if (nombres == null || nombres.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol.");
        }
        Set<Rol> roles = new HashSet<>();
        for (RolUsuario nombre : nombres) {
            roles.add(entityManager.getReference(Rol.class, rolService.idDe(nombre)));
        }
        return roles;
    }

    private void validarQueQuedeOtroAdministrador(String mensaje) {
        if (usuarioRepository.countByRol(RolUsuario.ADMIN) <= 1) {
            throw new ConflictException(mensaje);
        }
    }

    private static boolean tieneRol(Usuario usuario, RolUsuario rol) {
        return usuario.getRoles().stream().anyMatch(r -> r.getNombre() == rol);
    }

    private static String normalizarUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        return username.trim();
    }

    private static void validarId(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("El ID debe ser un número positivo.");
        }
    }

    private static Specification<Usuario> busqueda(String query) {
        if (query == null || query.isBlank()) {
            return (root, criteria, builder) -> builder.conjunction();
        }
        String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, criteria, builder) -> {
            var persona = root.join("persona", JoinType.LEFT);
            return builder.or(
                    builder.like(builder.lower(root.get("username")), pattern),
                    builder.like(builder.lower(persona.get("nombreCompleto")), pattern),
                    builder.like(builder.lower(persona.get("ci")), pattern),
                    builder.like(builder.lower(persona.get("ruc")), pattern));
        };
    }

    private static Pageable conOrdenPermitido(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!ORDENES_PERMITIDOS.contains(order.getProperty())) {
                throw new IllegalArgumentException("No se puede ordenar por '" + order.getProperty()
                        + "'. Valores permitidos: " + ORDENES_PERMITIDOS + ".");
            }
        }
        return pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
    }
}
