package com.patitas.turno.domain.exception;

public class RemoteServiceUnavailableException extends RuntimeException {

    public RemoteServiceUnavailableException(String mensaje) {
        super(mensaje);
    }

    public RemoteServiceUnavailableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
