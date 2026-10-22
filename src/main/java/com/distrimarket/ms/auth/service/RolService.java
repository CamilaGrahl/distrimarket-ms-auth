package com.distrimarket.ms.auth.service;

import com.distrimarket.commons.entity.Rol;
import com.distrimarket.commons.enums.RolUsuario;
import com.distrimarket.ms.auth.config.CacheConfig;
import com.distrimarket.ms.auth.repository.RolRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolService {

    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    /**
     * Id de un rol en la tabla roles. Es un catálogo fijo (lo carga la migración), por eso su
     * sección de caché es persistente: se consulta una sola vez por rol.
     */
    @Cacheable(cacheNames = CacheConfig.ROL, key = "#rol.name()")
    @Transactional(readOnly = true)
    public Long idDe(RolUsuario rol) {
        return rolRepository.findByNombre(rol)
                .map(Rol::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "El rol " + rol + " no está cargado en la tabla roles (migración 005)."));
    }
}
