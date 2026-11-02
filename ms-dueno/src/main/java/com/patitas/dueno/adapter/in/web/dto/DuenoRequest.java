package com.patitas.dueno.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DuenoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        @Schema(example = "Ana") String nombre,
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido admite hasta 100 caracteres")
        @Schema(example = "Gómez") String apellido,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        @Size(max = 254, message = "El email admite hasta 254 caracteres")
        @Schema(example = "ana@example.com") String email,
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono admite hasta 30 caracteres")
        @Schema(example = "1112345678") String telefono) {
}
