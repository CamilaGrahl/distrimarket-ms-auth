package com.distrimarket.ms.auth.controller;

import com.distrimarket.commons.enums.RolUsuario;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Roles asignables. Están definidos por el enum RolUsuario. */
@RestController
@RequestMapping("/roles")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auth-Usuarios")
public class RolController {

    @GetMapping
    public List<RolUsuario> listar() {
        return List.of(RolUsuario.values());
    }
}
