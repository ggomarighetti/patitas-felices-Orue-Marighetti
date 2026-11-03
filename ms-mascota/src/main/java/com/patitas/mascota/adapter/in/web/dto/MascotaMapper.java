package com.patitas.mascota.adapter.in.web.dto;

import com.patitas.mascota.domain.model.Mascota;

public final class MascotaMapper {

    private MascotaMapper() {
    }

    public static Mascota toEntity(MascotaRequest request) {
        Mascota value = new Mascota();
        value.setNombre(request.getNombre());
        value.setEspecie(request.getEspecie());
        value.setRaza(request.getRaza());
        value.setFechaNacimiento(request.getFechaNacimiento());
        value.setEdad(request.getEdad());
        value.setDuenoId(request.getDuenoId());
        return value;
    }

    public static MascotaResponse toResponse(Mascota value) {
        return new MascotaResponse(value.getId(), value.getNombre(), value.getEspecie(), value.getRaza(), value.getFechaNacimiento(), value.getEdad(), value.getDuenoId());
    }
}
