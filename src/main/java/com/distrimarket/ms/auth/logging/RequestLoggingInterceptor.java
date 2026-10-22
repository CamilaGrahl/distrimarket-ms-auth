package com.distrimarket.ms.auth.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Registra cada solicitud con el usuario que la hizo (tomado del token) y su duración. */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = RequestLoggingInterceptor.class.getName() + ".startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
        log.info("Solicitud entrante: {} {} usuario={}", request.getMethod(), request.getRequestURI(), usuario());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        if (request.getAttribute(START_TIME_ATTRIBUTE) instanceof Long startedAt) {
            log.info("Solicitud completada: {} {} status={} duración={}ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(),
                    (System.nanoTime() - startedAt) / 1_000_000);
        }
    }

    private static String usuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || auth instanceof AnonymousAuthenticationToken ? "anónimo" : auth.getName();
    }
}
