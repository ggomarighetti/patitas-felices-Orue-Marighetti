package com.patitas.veterinario.domain.port.in;

import com.patitas.veterinario.domain.model.Veterinario;
import java.util.List;
import java.util.Optional;

public interface VeterinarioService {

    List<Veterinario> findAll();

    Optional<Veterinario> findById(Long id);

    List<Veterinario> findByEspecialidad(String especialidad);

    Veterinario save(Veterinario veterinario);

    Veterinario update(Long id, Veterinario veterinario);

    void deleteById(Long id);
}
