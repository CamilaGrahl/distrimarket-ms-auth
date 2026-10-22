package com.distrimarket.ms.auth.security;

import com.distrimarket.commons.dto.TokenInfoDTO;
import com.distrimarket.commons.dto.TokenResponseDTO;
import com.distrimarket.commons.enums.RolUsuario;
import com.distrimarket.commons.security.JwtClaims;
import com.distrimarket.ms.auth.config.JwtConfig;
import com.distrimarket.ms.auth.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "clave-de-prueba-con-mas-de-32-caracteres!";

    private static SecurityProperties properties(String secret, String issuer, Duration expiration) {
        return new SecurityProperties(new SecurityProperties.Jwt(secret, issuer, expiration),
                null, null, null, null);
    }

    private final SecurityProperties props = properties(SECRET, "distrimarket-auth", Duration.ofMinutes(30));
    private final JwtConfig jwtConfig = new JwtConfig(props);
    private final JwtTokenService service = new JwtTokenService(jwtConfig.jwtEncoder(), props);
    private final JwtDecoder decoder = jwtConfig.jwtDecoder();

    private final UsuarioAutenticado admin = new UsuarioAutenticado(
            7L, "admin", "hash", List.of(RolUsuario.ADMIN, RolUsuario.VENDEDOR), 10L);

    @Test
    void elTokenLlevaUsuarioRolesYDatosDePersonaYEmpleado() {
        TokenResponseDTO token = service.emitir(admin, 3L);

        assertThat(token.getTokenType()).isEqualTo("Bearer");
        assertThat(token.getExpiresIn()).isEqualTo(1800);
        Jwt jwt = decoder.decode(token.getAccessToken());
        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.getClaimAsStringList(JwtClaims.ROLES)).containsExactly("ADMIN", "VENDEDOR");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("distrimarket-auth");

        TokenInfoDTO info = service.describir(jwt);
        assertThat(info.getIdUsuario()).isEqualTo(7L);
        assertThat(info.getIdPersona()).isEqualTo(10L);
        assertThat(info.getIdEmpleado()).isEqualTo(3L);
        assertThat(info.getRoles()).containsExactly(RolUsuario.ADMIN, RolUsuario.VENDEDOR);
    }

    @Test
    void unTokenVencidoSeRechaza() {
        Clock haceUnaHora = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        String vencido = new JwtTokenService(jwtConfig.jwtEncoder(), props, haceUnaHora)
                .emitir(admin, null).getAccessToken();

        assertThatThrownBy(() -> decoder.decode(vencido)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        SecurityProperties otra = properties("otra-clave-distinta-tambien-de-32-caracteres", "distrimarket-auth",
                Duration.ofMinutes(30));
        String ajeno = new JwtTokenService(new JwtConfig(otra).jwtEncoder(), otra).emitir(admin, null).getAccessToken();

        assertThatThrownBy(() -> decoder.decode(ajeno)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenDeOtroEmisorSeRechaza() {
        SecurityProperties otroEmisor = properties(SECRET, "otro-sistema", Duration.ofMinutes(30));
        String ajeno = new JwtTokenService(new JwtConfig(otroEmisor).jwtEncoder(), otroEmisor)
                .emitir(admin, null).getAccessToken();

        assertThatThrownBy(() -> decoder.decode(ajeno)).isInstanceOf(JwtException.class);
    }

    @Test
    void unaClaveCortaNoPermiteArrancar() {
        assertThatThrownBy(() -> new SecurityProperties.Jwt("corta", "x", Duration.ofMinutes(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
