package com.patitas.veterinario.domain.port.out;

import com.patitas.veterinario.domain.model.Veterinario;
import java.util.List;
import java.util.Optional;

public interface VeterinarioRepository {

    List<Veterinario> findAll();

    Optional<Veterinario> findById(Long id);

    List<Veterinario> findByEspecialidad(String especialidad);

    Veterinario save(Veterinario value);

    void deleteById(Long id);
}
