package com.distrimarket.ms.auth.config;

import com.distrimarket.commons.dto.UsuarioResponseDTO;
import com.distrimarket.commons.enums.RolUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Lo que se guarda en Redis tiene que volver como el mismo tipo de objeto. */
class CacheSerializationTest {

    private final RedisSerializer<Object> serializer = CacheConfig.valueSerializer();

    @SuppressWarnings("unchecked")
    private <T> T roundTrip(T value) {
        return (T) serializer.deserialize(serializer.serialize(value));
    }

    @Test
    void unDtoVuelveConSuTipoYSusRoles() {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setIdUsuario(5L);
        dto.setUsername("jperez");
        dto.setRoles(List.of(RolUsuario.ADMIN, RolUsuario.VENDEDOR));
        dto.setActivo(true);

        UsuarioResponseDTO leido = roundTrip(dto);

        assertThat(leido).isInstanceOf(UsuarioResponseDTO.class).isEqualTo(dto);
        assertThat(new String(serializer.serialize(dto), StandardCharsets.UTF_8)).doesNotContain("password");
    }

    @Test
    void numerosFechasYMapasConservanSuTipo() {
        assertThat(roundTrip(42L)).isInstanceOf(Long.class).isEqualTo(42L);

        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("fechaIngreso", LocalDate.of(2026, 10, 22));
        mapa.put("monto", new BigDecimal("15000.50"));
        Map<String, Object> leido = roundTrip(mapa);

        assertThat(leido.get("fechaIngreso")).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(leido.get("monto")).isEqualTo(new BigDecimal("15000.50"));
    }
}
