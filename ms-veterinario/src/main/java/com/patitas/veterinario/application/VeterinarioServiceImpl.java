package com.patitas.veterinario.application;

import com.patitas.veterinario.domain.exception.ResourceNotFoundException;
import com.patitas.veterinario.domain.model.Veterinario;
import com.patitas.veterinario.domain.port.in.VeterinarioService;
import com.patitas.veterinario.domain.port.out.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VeterinarioServiceImpl implements VeterinarioService {

    private final VeterinarioRepository repository;

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
        value.setId(null);
        return repository.save(value);
    }

    @Override
    @Transactional
    public Veterinario update(Long id, Veterinario value) {
        Veterinario stored = requireResource(id);
        stored.setNombre(value.getNombre());
        stored.setApellido(value.getApellido());
        stored.setMatricula(value.getMatricula());
        stored.setEspecialidad(value.getEspecialidad());
        stored.setEmail(value.getEmail());
        return repository.save(stored);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        requireResource(id);
        repository.deleteById(id);
    }

    private Veterinario requireResource(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado: " + id));
    }
}
