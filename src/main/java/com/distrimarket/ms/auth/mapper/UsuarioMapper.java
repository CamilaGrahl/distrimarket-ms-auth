package com.distrimarket.ms.auth.mapper;

import com.distrimarket.commons.dto.UsuarioResponseDTO;
import com.distrimarket.commons.entity.Empleado;
import com.distrimarket.commons.entity.Persona;
import com.distrimarket.commons.entity.Rol;
import com.distrimarket.commons.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/** Arma la respuesta pública de un usuario: nunca incluye la contraseña. */
@Component
public class UsuarioMapper {

    public UsuarioResponseDTO toResponse(Usuario usuario, Empleado empleado) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setIdUsuario(usuario.getId());
        dto.setUsername(usuario.getUsername());
        dto.setActivo(usuario.getActivo());
        dto.setRoles(usuario.getRoles().stream()
                .map(Rol::getNombre)
                .sorted(Comparator.naturalOrder())
                .toList());
        Persona persona = usuario.getPersona();
        if (persona != null) {
            dto.setIdPersona(persona.getId());
            dto.setNombreCompleto(persona.getNombreCompleto());
            dto.setDocumento(persona.getCi() != null ? persona.getCi() : persona.getRuc());
        }
        if (empleado != null) {
            dto.setIdEmpleado(empleado.getId());
            dto.setCargo(empleado.getCargo());
        }
        return dto;
    }
}
