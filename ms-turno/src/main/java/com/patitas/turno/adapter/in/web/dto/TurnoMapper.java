package com.patitas.turno.adapter.in.web.dto;

import com.patitas.turno.domain.model.Turno;

public final class TurnoMapper {

    private TurnoMapper() {
    }

    public static Turno toEntity(TurnoRequest request) {
        Turno value = new Turno();
        value.setFechaHora(request.getFechaHora());
        value.setMascotaId(request.getMascotaId());
        value.setVeterinarioId(request.getVeterinarioId());
        value.setMotivo(request.getMotivo());
        return value;
    }

    public static TurnoResponse toResponse(Turno value) {
        return new TurnoResponse(value.getId(), value.getFechaHora(), value.getMascotaId(), value.getVeterinarioId(), value.getMotivo(), value.getEstado());
    }
}
