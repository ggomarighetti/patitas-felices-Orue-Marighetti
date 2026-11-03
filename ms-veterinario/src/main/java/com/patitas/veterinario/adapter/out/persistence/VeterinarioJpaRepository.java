package com.patitas.veterinario.adapter.out.persistence;

import com.patitas.veterinario.domain.model.Veterinario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VeterinarioJpaRepository extends JpaRepository<Veterinario, Long> {

    List<Veterinario> findByEspecialidad(String especialidad);
}
