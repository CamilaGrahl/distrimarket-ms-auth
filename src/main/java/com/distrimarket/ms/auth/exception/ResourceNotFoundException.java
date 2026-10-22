package com.distrimarket.ms.auth.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s con ID %s no encontrado.".formatted(resource, id));
    }
}
