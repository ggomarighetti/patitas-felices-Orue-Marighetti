package com.patitas.mascota.domain.port.in;

import com.patitas.mascota.domain.model.Mascota;
import java.util.List;
import java.util.Optional;

public interface MascotaService {

    List<Mascota> findAll();

    Optional<Mascota> findById(Long id);

    List<Mascota> findByDuenoId(Long duenoId);

    Mascota save(Mascota mascota);

    Mascota update(Long id, Mascota mascota);

    void deleteById(Long id);
}
