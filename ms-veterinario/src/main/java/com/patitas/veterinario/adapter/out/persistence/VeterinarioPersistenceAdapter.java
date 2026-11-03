package com.patitas.veterinario.adapter.out.persistence;

import com.patitas.veterinario.domain.model.Veterinario;
import com.patitas.veterinario.domain.port.out.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VeterinarioPersistenceAdapter implements VeterinarioRepository {

    private final VeterinarioJpaRepository repository;

    @Override
    public List<Veterinario> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Veterinario> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Veterinario> findByEspecialidad(String especialidad) {
        return repository.findByEspecialidad(especialidad);
    }

    @Override
    public Veterinario save(Veterinario value) {
        return repository.saveAndFlush(value);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
