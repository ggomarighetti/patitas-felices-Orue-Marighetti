package com.patitas.mascota.adapter.out.persistence;

import com.patitas.mascota.domain.model.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MascotaJpaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByDuenoId(Long duenoId);
}
