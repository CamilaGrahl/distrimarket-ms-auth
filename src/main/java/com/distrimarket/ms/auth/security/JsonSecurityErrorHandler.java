package com.distrimarket.ms.auth.security;

import com.distrimarket.ms.auth.exception.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respuestas 401 y 403 de los filtros de seguridad: JSON claro, sin trazas del sistema.
 * Los rechazos de @PreAuthorize los atiende GlobalExceptionHandler con el mismo formato.
 */
@Slf4j
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        boolean tokenInvalido = exception instanceof InvalidBearerTokenException;
        String message = tokenInvalido
                ? "El token no es válido o ya expiró. Inicie sesión nuevamente."
                : "Se requiere autenticación: envíe el token en el encabezado Authorization (Bearer).";
        log.warn("No autenticado: {} {} -> 401 ({})",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, request, HttpStatus.UNAUTHORIZED, message);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.warn("Acceso denegado: {} {} usuario={} autoridades={} -> 403",
                request.getMethod(), request.getRequestURI(),
                auth == null ? "anónimo" : auth.getName(),
                auth == null ? "[]" : auth.getAuthorities());
        write(response, request, HttpStatus.FORBIDDEN,
                "No tiene permisos para acceder a este recurso.");
    }

    private void write(HttpServletResponse response, HttpServletRequest request,
                       HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
