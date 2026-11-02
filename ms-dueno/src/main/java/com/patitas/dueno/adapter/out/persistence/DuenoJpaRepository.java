package com.patitas.dueno.adapter.out.persistence;

import com.patitas.dueno.domain.model.Dueno;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuenoJpaRepository extends JpaRepository<Dueno, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
