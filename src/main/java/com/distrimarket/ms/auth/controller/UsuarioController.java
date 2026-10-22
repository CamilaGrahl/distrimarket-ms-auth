package com.distrimarket.ms.auth.controller;

import com.distrimarket.commons.dto.UsuarioCreateRequestDTO;
import com.distrimarket.commons.dto.UsuarioPageResponseDTO;
import com.distrimarket.commons.dto.UsuarioResponseDTO;
import com.distrimarket.commons.dto.UsuarioUpdateRequestDTO;
import com.distrimarket.ms.auth.service.UsuarioService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de usuarios y asignación de roles. Solo ADMIN (también lo exige la regla de URL). */
@RestController
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auth-Usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public UsuarioPageResponseDTO listar(
            @RequestParam(required = false, name = "q") String query,
            @Parameter(hidden = true) Pageable pageable) {
        Page<UsuarioResponseDTO> page = usuarioService.listar(query, pageable);
        UsuarioPageResponseDTO response = new UsuarioPageResponseDTO();
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setIsFirst(page.isFirst());
        response.setIsLast(page.isLast());
        response.setContent(page.getContent());
        return response;
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO crear(@Valid @RequestBody UsuarioCreateRequestDTO request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequestDTO request) {
        return usuarioService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.eliminar(id, jwt.getSubject());
    }
}
