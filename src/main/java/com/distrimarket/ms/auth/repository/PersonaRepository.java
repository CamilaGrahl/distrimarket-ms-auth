package com.distrimarket.ms.auth.repository;

import com.distrimarket.commons.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByCi(String ci);
}
