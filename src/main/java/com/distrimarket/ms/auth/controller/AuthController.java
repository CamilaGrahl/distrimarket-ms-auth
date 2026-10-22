package com.distrimarket.ms.auth.controller;

import com.distrimarket.commons.dto.LoginRequestDTO;
import com.distrimarket.commons.dto.TokenInfoDTO;
import com.distrimarket.commons.dto.TokenResponseDTO;
import com.distrimarket.commons.dto.UsuarioResponseDTO;
import com.distrimarket.ms.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Rutas bajo /auth: el login es público, las demás necesitan un token válido. */
@RestController
@Tag(name = "Auth-Sesion")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }

    @GetMapping("/validar")
    @PreAuthorize("isAuthenticated()")
    public TokenInfoDTO validar(@AuthenticationPrincipal Jwt jwt) {
        return authService.validar(jwt);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UsuarioResponseDTO usuarioActual(@AuthenticationPrincipal Jwt jwt) {
        return authService.usuarioActual(jwt);
    }
}
