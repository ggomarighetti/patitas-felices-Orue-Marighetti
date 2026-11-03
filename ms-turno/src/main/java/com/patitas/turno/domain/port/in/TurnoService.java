package com.patitas.turno.domain.port.in;

import com.patitas.turno.domain.model.Turno;
import java.util.List;
import java.util.Optional;

public interface TurnoService {

    List<Turno> findAll();

    Optional<Turno> findById(Long id);

    List<Turno> findByVeterinarioId(Long veterinarioId);

    List<Turno> findByMascotaId(Long mascotaId);

    Turno save(Turno value);

    Turno update(Long id, Turno value);

    Turno cancelar(Long id);

    void deleteById(Long id);
}
