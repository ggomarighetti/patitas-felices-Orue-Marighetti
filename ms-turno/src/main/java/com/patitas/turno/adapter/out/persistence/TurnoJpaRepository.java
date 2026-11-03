package com.patitas.turno.adapter.out.persistence;

import com.patitas.turno.domain.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TurnoJpaRepository extends JpaRepository<Turno, Long> {

    List<Turno> findByVeterinarioId(Long veterinarioId);

    List<Turno> findByMascotaId(Long mascotaId);
}
