package com.distrimarket.ms.auth.security;

/**
 * Nombres de los datos que viajan dentro del token. El gateway y los microservicios leen
 * {@link #ROLES} para autorizar; los demás sirven para saber quién hace el pedido.
 */
public final class JwtClaims {

    public static final String ROLES = "roles";
    public static final String USER_ID = "uid";
    public static final String PERSONA_ID = "idPersona";
    public static final String EMPLEADO_ID = "idEmpleado";

    private JwtClaims() {
    }
}
