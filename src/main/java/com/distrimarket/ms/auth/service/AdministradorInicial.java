package com.distrimarket.ms.auth.service;

import com.distrimarket.commons.entity.Persona;
import com.distrimarket.commons.entity.Rol;
import com.distrimarket.commons.entity.Usuario;
import com.distrimarket.commons.enums.RolUsuario;
import com.distrimarket.commons.enums.TipoPersona;
import com.distrimarket.ms.auth.config.SecurityProperties;
import com.distrimarket.ms.auth.repository.PersonaRepository;
import com.distrimarket.ms.auth.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Al arrancar, si no hay ningún administrador activo, crea el configurado en
 * {@code security.bootstrap-admin}. Sin él nadie podría registrar el primer usuario.
 */
@Slf4j
@Component
public class AdministradorInicial implements ApplicationRunner {

    private final SecurityProperties.BootstrapAdmin config;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolService rolService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public AdministradorInicial(SecurityProperties properties,
                                UsuarioRepository usuarioRepository,
                                PersonaRepository personaRepository,
                                RolService rolService,
                                PasswordEncoder passwordEncoder,
                                EntityManager entityManager) {
        this.config = properties.bootstrapAdmin();
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.rolService = rolService;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (config == null || !config.enabled()) {
            return;
        }
        if (usuarioRepository.countByRol(RolUsuario.ADMIN) > 0) {
            log.info("Ya existe un administrador activo; no se crea el administrador inicial");
            return;
        }
        if (usuarioRepository.existsUsername(config.username())) {
            log.warn("No hay administradores activos y el usuario '{}' ya existe (dado de baja o sin rol ADMIN); "
                    + "revise la tabla usuarios", config.username());
            return;
        }
        Persona persona = personaRepository.findByCi(config.ci())
                .orElseGet(() -> personaRepository.save(Persona.builder()
                        .tipoPersona(TipoPersona.FISICA)
                        .nombreCompleto(config.nombreCompleto())
                        .ci(config.ci())
                        .activo(true)
                        .build()));
        if (usuarioRepository.existsByPersonaIncluyendoInactivos(persona.getId())) {
            log.warn("La persona con CI {} ya tiene un usuario; no se crea el administrador inicial", config.ci());
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .username(config.username())
                .password(passwordEncoder.encode(config.password()))
                .persona(persona)
                .roles(Set.of(entityManager.getReference(Rol.class, rolService.idDe(RolUsuario.ADMIN))))
                .activo(true)
                .build());
        log.warn("Se creó el administrador inicial '{}'. Fuera de desarrollo defina AUTH_ADMIN_PASSWORD "
                + "o cambie la contraseña con PUT /auth/usuarios/<id>.", config.username());
    }
}
