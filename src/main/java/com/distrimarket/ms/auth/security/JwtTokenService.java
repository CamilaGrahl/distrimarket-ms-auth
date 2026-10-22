package com.distrimarket.ms.auth.security;

import com.distrimarket.commons.dto.TokenInfoDTO;
import com.distrimarket.commons.dto.TokenResponseDTO;
import com.distrimarket.commons.enums.RolUsuario;
import com.distrimarket.ms.auth.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** Emite el token del login y traduce un token ya validado a su información legible. */
@Slf4j
@Service
public class JwtTokenService {

    private static final String TOKEN_TYPE = "Bearer";

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties.Jwt jwtProperties;
    private final Clock clock;

    @Autowired
    public JwtTokenService(JwtEncoder jwtEncoder, SecurityProperties properties) {
        this(jwtEncoder, properties, Clock.systemUTC());
    }

    JwtTokenService(JwtEncoder jwtEncoder, SecurityProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = properties.jwt();
        this.clock = clock;
    }

    public TokenResponseDTO emitir(UsuarioAutenticado usuario, Long idEmpleado) {
        Instant ahora = clock.instant();
        Instant vence = ahora.plus(jwtProperties.expiration());
        List<String> roles = usuario.roles().stream().map(Enum::name).toList();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.issuer())
                .subject(usuario.username())
                .issuedAt(ahora)
                .expiresAt(vence)
                .claim(JwtClaims.USER_ID, usuario.id())
                .claim(JwtClaims.ROLES, roles);
        if (usuario.idPersona() != null) {
            claims.claim(JwtClaims.PERSONA_ID, usuario.idPersona());
        }
        if (idEmpleado != null) {
            claims.claim(JwtClaims.EMPLEADO_ID, idEmpleado);
        }

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        log.info("Token emitido para {} con roles {} (vence {})", usuario.username(), roles, vence);

        TokenResponseDTO response = new TokenResponseDTO();
        response.setAccessToken(token);
        response.setTokenType(TOKEN_TYPE);
        response.setExpiresIn(jwtProperties.expiration().toSeconds());
        response.setExpiresAt(OffsetDateTime.ofInstant(vence, ZoneOffset.UTC));
        response.setUsername(usuario.username());
        response.setRoles(usuario.roles());
        response.setIdPersona(usuario.idPersona());
        response.setIdEmpleado(idEmpleado);
        return response;
    }

    public TokenInfoDTO describir(Jwt jwt) {
        TokenInfoDTO info = new TokenInfoDTO();
        info.setValido(true);
        info.setIdUsuario(idUsuario(jwt));
        info.setUsername(jwt.getSubject());
        info.setRoles(roles(jwt));
        info.setIdPersona(numero(jwt, JwtClaims.PERSONA_ID));
        info.setIdEmpleado(numero(jwt, JwtClaims.EMPLEADO_ID));
        info.setEmitidoEn(jwt.getIssuedAt() == null ? null : OffsetDateTime.ofInstant(jwt.getIssuedAt(), ZoneOffset.UTC));
        info.setExpiraEn(jwt.getExpiresAt() == null ? null : OffsetDateTime.ofInstant(jwt.getExpiresAt(), ZoneOffset.UTC));
        return info;
    }

    public static Long idUsuario(Jwt jwt) {
        return numero(jwt, JwtClaims.USER_ID);
    }

    private static List<RolUsuario> roles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(JwtClaims.ROLES);
        return roles == null ? List.of() : roles.stream().map(RolUsuario::valueOf).toList();
    }

    private static Long numero(Jwt jwt, String claim) {
        Object value = jwt.getClaim(claim);
        return value instanceof Number number ? number.longValue() : null;
    }
}
