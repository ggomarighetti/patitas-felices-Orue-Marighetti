package com.patitas.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,50}") String username,
        @NotBlank @Size(min = 4, max = 72) String password,
        @NotBlank @Pattern(regexp = "ADMIN|VET|RECEPCIONISTA") String rol) {
}
