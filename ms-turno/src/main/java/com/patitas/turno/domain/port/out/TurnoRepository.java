package com.patitas.turno.domain.port.out;

import com.patitas.turno.domain.model.Turno;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository {

    List<Turno> findAll();

    Optional<Turno> findById(Long id);

    List<Turno> findByVeterinarioId(Long veterinarioId);

    List<Turno> findByMascotaId(Long mascotaId);

    Turno save(Turno turno);

    Turno cancelar(Long id);
}
