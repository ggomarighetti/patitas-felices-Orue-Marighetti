package com.patitas.veterinario.adapter.in.web.dto;

import com.patitas.veterinario.domain.model.Veterinario;

public final class VeterinarioMapper {

    private VeterinarioMapper() {
    }

    public static Veterinario toEntity(VeterinarioRequest request) {
        Veterinario value = new Veterinario();
        value.setNombre(request.getNombre());
        value.setApellido(request.getApellido());
        value.setMatricula(request.getMatricula());
        value.setEspecialidad(request.getEspecialidad());
        value.setEmail(request.getEmail());
        return value;
    }

    public static VeterinarioResponse toResponse(Veterinario value) {
        return new VeterinarioResponse(value.getId(), value.getNombre(), value.getApellido(), value.getMatricula(), value.getEspecialidad(), value.getEmail());
    }
}
