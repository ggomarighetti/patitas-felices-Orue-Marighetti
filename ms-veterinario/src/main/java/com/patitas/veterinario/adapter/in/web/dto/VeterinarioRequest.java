package com.patitas.veterinario.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class VeterinarioRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    private String apellido;

    @Size(max = 50)
    private String matricula;

    @NotBlank
    @Size(max = 100)
    private String especialidad;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;
}
