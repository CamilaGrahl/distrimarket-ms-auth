package com.distrimarket.ms.auth.security;

import com.distrimarket.commons.enums.RolUsuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Usuario que inicia sesión, tal como lo necesita Spring Security para validar la contraseña. */
public record UsuarioAutenticado(
        Long id,
        String username,
        String passwordHash,
        List<RolUsuario> roles,
        Long idPersona) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.name())).toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // Solo se cargan usuarios activos: los dados de baja no llegan hasta aquí.
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        // Nunca se imprime el hash de la contraseña.
        return "UsuarioAutenticado[id=" + id + ", username=" + username + ", roles=" + roles + "]";
    }
}
