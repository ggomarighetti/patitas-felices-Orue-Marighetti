package com.patitas.dueno.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuenoResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
}
