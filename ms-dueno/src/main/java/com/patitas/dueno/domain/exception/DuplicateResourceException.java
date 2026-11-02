package com.patitas.dueno.domain.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String mensaje) {
        super(mensaje);
    }

    public DuplicateResourceException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
