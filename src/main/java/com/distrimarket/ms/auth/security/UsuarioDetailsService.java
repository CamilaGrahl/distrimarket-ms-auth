package com.distrimarket.ms.auth.security;

import com.distrimarket.commons.entity.Rol;
import com.distrimarket.commons.entity.Usuario;
import com.distrimarket.ms.auth.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

/**
 * Carga las credenciales para el login directamente de la base. A propósito no usa caché:
 * el hash de la contraseña no se guarda en Redis.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioAutenticado loadUserByUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username == null ? "" : username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.getRoles().stream().map(Rol::getNombre).sorted(Comparator.naturalOrder()).toList(),
                usuario.getPersona() == null ? null : usuario.getPersona().getId());
    }
}
