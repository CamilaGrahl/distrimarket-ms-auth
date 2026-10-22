package com.distrimarket.ms.auth.config;

import com.distrimarket.ms.auth.security.JsonSecurityErrorHandler;
import com.distrimarket.ms.auth.security.UsuarioDetailsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad a nivel de URL (reglas de application.yml) y a nivel de método (@PreAuthorize,
 * habilitado con {@link EnableMethodSecurity}). Sin sesiones: cada pedido trae su token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityProperties properties,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            JsonSecurityErrorHandler errorHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    properties.publicPaths().forEach(path -> auth.requestMatchers(path).permitAll());
                    for (SecurityProperties.UrlRule rule : properties.rules()) {
                        String[] roles = rule.roles().stream().map(Enum::name).toArray(String[]::new);
                        if (rule.methods().isEmpty()) {
                            auth.requestMatchers(rule.pattern()).hasAnyRole(roles);
                        } else {
                            rule.methods().forEach(method -> auth
                                    .requestMatchers(HttpMethod.valueOf(method.toUpperCase()), rule.pattern())
                                    .hasAnyRole(roles));
                        }
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
        return http.build();
    }

    /** BCrypt con costo configurable: la contraseña nunca se guarda ni se compara en texto plano. */
    @Bean
    public PasswordEncoder passwordEncoder(SecurityProperties properties) {
        return new BCryptPasswordEncoder(properties.password().bcryptStrength());
    }

    /** Valida usuario y contraseña en el login contra la tabla usuarios. */
    @Bean
    public AuthenticationManager authenticationManager(
            UsuarioDetailsService usuarioDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(usuarioDetailsService);
        return new ProviderManager(provider);
    }
}
