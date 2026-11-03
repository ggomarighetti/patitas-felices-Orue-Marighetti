package com.patitas.veterinario.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class VeterinarioResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String matricula;
    private String especialidad;
    private String email;
}
