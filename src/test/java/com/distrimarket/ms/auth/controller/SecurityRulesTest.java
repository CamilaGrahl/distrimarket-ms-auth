package com.distrimarket.ms.auth.controller;

import com.distrimarket.commons.dto.TokenResponseDTO;
import com.distrimarket.commons.dto.UsuarioResponseDTO;
import com.distrimarket.commons.enums.RolUsuario;
import com.distrimarket.ms.auth.config.JwtConfig;
import com.distrimarket.ms.auth.config.SecurityConfig;
import com.distrimarket.ms.auth.config.SecurityProperties;
import com.distrimarket.ms.auth.exception.GlobalExceptionHandler;
import com.distrimarket.ms.auth.security.JsonSecurityErrorHandler;
import com.distrimarket.ms.auth.security.JwtTokenService;
import com.distrimarket.ms.auth.security.UsuarioAutenticado;
import com.distrimarket.ms.auth.security.UsuarioDetailsService;
import com.distrimarket.ms.auth.service.AuthService;
import com.distrimarket.ms.auth.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorre la cadena de seguridad real (filtros + @PreAuthorize) con tokens firmados de verdad. */
@WebMvcTest(controllers = {AuthController.class, UsuarioController.class, RolController.class})
@Import({SecurityConfig.class, JwtConfig.class, JsonSecurityErrorHandler.class, GlobalExceptionHandler.class})
class SecurityRulesTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private SecurityProperties properties;
    @Autowired
    private JwtConfig jwtConfig;

    @MockBean
    private AuthService authService;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private UsuarioDetailsService usuarioDetailsService;

    private String token(RolUsuario... roles) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(1L, "prueba", "x", List.of(roles), 1L);
        return "Bearer " + new JwtTokenService(jwtConfig.jwtEncoder(), properties).emitir(usuario, null).getAccessToken();
    }

    @Test
    void elLoginEsPublico() throws Exception {
        TokenResponseDTO respuesta = new TokenResponseDTO();
        respuesta.setAccessToken("abc");
        when(authService.login(any())).thenReturn(respuesta);

        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("abc"));
    }

    @Test
    void credencialesIncorrectasDevuelven401SinTraza() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos."))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message", containsString("Authorization")));
    }

    @Test
    void tokenAlteradoDevuelve401() throws Exception {
        mvc.perform(get("/usuarios").header("Authorization", token(RolUsuario.ADMIN) + "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("no es válido")));
    }

    @Test
    void unVendedorNoAdministraUsuarios() throws Exception {
        mvc.perform(get("/usuarios").header("Authorization", token(RolUsuario.VENDEDOR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(delete("/usuarios/2").header("Authorization", token(RolUsuario.VENDEDOR)))
                .andExpect(status().isForbidden());
        verify(usuarioService, never()).eliminar(any(), any());
    }

    @Test
    void unAdministradorConVariosRolesAccede() throws Exception {
        UsuarioResponseDTO usuario = new UsuarioResponseDTO();
        usuario.setIdUsuario(2L);
        when(usuarioService.obtener(2L)).thenReturn(usuario);

        mvc.perform(get("/usuarios/2").header("Authorization", token(RolUsuario.VENDEDOR, RolUsuario.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(2));
        mvc.perform(delete("/usuarios/2").header("Authorization", token(RolUsuario.ADMIN)))
                .andExpect(status().isNoContent());
        verify(usuarioService).eliminar(eq(2L), eq("prueba"));
    }

    @Test
    void cualquierUsuarioAutenticadoConsultaSuSesion() throws Exception {
        mvc.perform(get("/me").header("Authorization", token(RolUsuario.COMPRADOR)))
                .andExpect(status().isOk());
        mvc.perform(get("/roles").header("Authorization", token(RolUsuario.COMPRADOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCuerpoInvalidoDevuelve400ConLosCampos() throws Exception {
        mvc.perform(post("/usuarios").header("Authorization", token(RolUsuario.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"password\":\"corta\",\"roles\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists())
                .andExpect(jsonPath("$.validationErrors.roles").exists());
    }
}
