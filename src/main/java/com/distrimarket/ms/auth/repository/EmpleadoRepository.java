package com.distrimarket.ms.auth.repository;

import com.distrimarket.commons.entity.Empleado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    /** Empleado activo de una persona: así un usuario queda vinculado a su legajo. */
    Optional<Empleado> findFirstByPersona_Id(Long idPersona);

    /** Empleados de todas las personas de una página, en una sola consulta. */
    @EntityGraph(attributePaths = "persona")
    List<Empleado> findByPersona_IdIn(Collection<Long> idsPersona);

    @EntityGraph(attributePaths = "persona")
    Optional<Empleado> findWithPersonaById(Long id);
}
