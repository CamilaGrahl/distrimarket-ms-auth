package com.distrimarket.ms.auth.repository;

import com.distrimarket.commons.entity.Rol;
import com.distrimarket.commons.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(RolUsuario nombre);
}
