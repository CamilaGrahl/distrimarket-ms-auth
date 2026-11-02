package com.distrimarket.ms.auth.service;

import com.distrimarket.commons.dto.LoginRequestDTO;
import com.distrimarket.commons.dto.TokenInfoDTO;
import com.distrimarket.commons.dto.TokenResponseDTO;
import com.distrimarket.commons.dto.UsuarioResponseDTO;
import com.distrimarket.ms.auth.exception.ResourceNotFoundException;
import com.distrimarket.ms.auth.security.JwtTokenService;
import com.distrimarket.ms.auth.security.UsuarioAutenticado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UsuarioService usuarioService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenService jwtTokenService,
                       UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.usuarioService = usuarioService;
    }

    /** Valida usuario y contraseña (BCrypt) y emite el token con sus roles. */
    public TokenResponseDTO login(LoginRequestDTO request) {
        String username = request.getUsername().trim();
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, request.getPassword()));
        } catch (AuthenticationException ex) {
            log.warn("Inicio de sesión fallido para '{}' ({})", username, ex.getClass().getSimpleName());
            throw ex;
        }
        UsuarioAutenticado usuario = (UsuarioAutenticado) authentication.getPrincipal();
        log.info("Inicio de sesión correcto de '{}'", usuario.username());
        // El token lleva el empleado y su depósito: cada microservicio opera sobre ese depósito.
        var empleado = usuarioService.empleadoDePersona(usuario.idPersona());
        return jwtTokenService.emitir(usuario,
                empleado.map(e -> e.getId()).orElse(null),
                empleado.map(e -> e.getDeposito() != null ? e.getDeposito().getId() : null).orElse(null));
    }

    /** Además de la firma y la vigencia (ya verificadas), confirma que el usuario siga activo. */
    public TokenInfoDTO validar(Jwt jwt) {
        usuarioActivo(jwt);
        return jwtTokenService.describir(jwt);
    }

    public UsuarioResponseDTO usuarioActual(Jwt jwt) {
        return usuarioActivo(jwt);
    }

    private UsuarioResponseDTO usuarioActivo(Jwt jwt) {
        Long idUsuario = JwtTokenService.idUsuario(jwt);
        if (idUsuario == null) {
            throw new InsufficientAuthenticationException("El token no identifica a un usuario.");
        }
        try {
            return usuarioService.obtener(idUsuario);
        } catch (ResourceNotFoundException ex) {
            log.warn("Token de '{}' rechazado: el usuario {} ya no está activo", jwt.getSubject(), idUsuario);
            throw new InsufficientAuthenticationException("El usuario del token ya no está activo.");
        }
    }
}
