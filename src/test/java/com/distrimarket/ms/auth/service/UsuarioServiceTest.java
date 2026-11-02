package com.distrimarket.ms.auth.service;

import com.distrimarket.ms.auth.config.DomainCache;
import com.distrimarket.ms.auth.mapper.UsuarioMapper;
import com.distrimarket.ms.auth.repository.EmpleadoRepository;
import com.distrimarket.ms.auth.repository.PersonaRepository;
import com.distrimarket.ms.auth.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PersonaRepository personaRepository = mock(PersonaRepository.class);
    private final EmpleadoRepository empleadoRepository = mock(EmpleadoRepository.class);
    private final RolService rolService = mock(RolService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UsuarioMapper mapper = mock(UsuarioMapper.class);
    private final DomainCache domainCache = mock(DomainCache.class);
    private final EntityManager entityManager = mock(EntityManager.class);

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, personaRepository, empleadoRepository, rolService,
                passwordEncoder, mapper, domainCache, entityManager);
        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(1)));
    }

    @Test
    void listarSinOrdenUsaIdAscendente() {
        service.listar(null, PageRequest.of(0, 10));

        assertThat(pageableUsado().getSort().toList()).containsExactly(Sort.Order.asc("id"));
    }

    @Test
    void listarConservaElOrdenPedidoYAgregaIdComoDesempate() {
        service.listar(null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "username")));

        Pageable usado = pageableUsado();
        assertThat(usado.getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("username", "id");
        assertThat(usado.getSort().getOrderFor("username").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(usado.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listarRechazaPropiedadesFueraDeLaAllowlist() {
        assertThatThrownBy(() -> service.listar(null, PageRequest.of(0, 10, Sort.by("password"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");

        verifyNoInteractions(usuarioRepository);
    }

    private Pageable pageableUsado() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioRepository).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }
}
