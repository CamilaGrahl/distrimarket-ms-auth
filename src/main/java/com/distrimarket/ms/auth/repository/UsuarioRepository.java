package com.distrimarket.ms.auth.repository;

import com.distrimarket.commons.entity.Usuario;
import com.distrimarket.commons.enums.RolUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Las consultas JPQL solo ven usuarios activos (@SQLRestriction en Usuario). */
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<Usuario> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"roles", "persona"})
    Optional<Usuario> findWithRolesById(Long id);

    /** Listado paginado con la persona ya cargada; evita una consulta extra por página. */
    @Override
    @EntityGraph(attributePaths = "persona")
    Page<Usuario> findAll(Specification<Usuario> spec, Pageable pageable);

    @Query("select count(distinct u) from Usuario u join u.roles r where r.nombre = :rol")
    long countByRol(@Param("rol") RolUsuario rol);

    // Consultas nativas: el username y la persona son únicos en la tabla, incluso entre usuarios dados de baja.
    @Query(value = "select exists(select 1 from usuarios where lower(username) = lower(:username))",
            nativeQuery = true)
    boolean existsUsername(@Param("username") String username);

    @Query(value = "select exists(select 1 from usuarios where lower(username) = lower(:username) and id <> :id)",
            nativeQuery = true)
    boolean existsUsernameAndIdNot(@Param("username") String username, @Param("id") Long id);

    @Query(value = "select exists(select 1 from usuarios where id_persona = :idPersona)", nativeQuery = true)
    boolean existsByPersonaIncluyendoInactivos(@Param("idPersona") Long idPersona);
}
