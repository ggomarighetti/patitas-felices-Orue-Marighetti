package com.patitas.turno.adapter.out.feign;

import lombok.Data;

@Data
public class MascotaResponse {

    private Long id;
    private String nombre;
    private Long duenoId;
}
