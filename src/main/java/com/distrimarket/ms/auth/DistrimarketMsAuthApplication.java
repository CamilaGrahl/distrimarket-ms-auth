package com.distrimarket.ms.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Microservicio de seguridad: administra usuarios y roles, valida credenciales y emite los
 * tokens JWT que el gateway y los demás microservicios verifican en cada pedido.
 */
@SpringBootApplication
@EntityScan("com.distrimarket.commons.entity")
public class DistrimarketMsAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistrimarketMsAuthApplication.class, args);
    }
}
