package com.patitas.auth.domain.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
