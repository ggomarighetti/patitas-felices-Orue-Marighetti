package com.patitas.dueno.adapter.in.web.dto;

import com.patitas.dueno.domain.model.Dueno;

public final class DuenoMapper {

    private DuenoMapper() {
    }

    public static Dueno toEntity(DuenoRequest request) {
        return new Dueno(null, request.getNombre(), request.getApellido(), request.getEmail(), request.getTelefono());
    }

    public static DuenoResponse toResponse(Dueno dueno) {
        return new DuenoResponse(dueno.getId(), dueno.getNombre(), dueno.getApellido(), dueno.getEmail(), dueno.getTelefono());
    }
}
