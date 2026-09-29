package com.patitas.dueno.domain.exception;

public class DuplicatedEmailException extends RuntimeException {

    public DuplicatedEmailException(String email) {
        super("Ya existe un dueño con el email: " + email);
    }
}
