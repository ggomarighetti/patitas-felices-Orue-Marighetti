package com.patitas.mascota.application;

import com.patitas.mascota.domain.exception.ResourceNotFoundException;
import com.patitas.mascota.domain.model.Mascota;
import com.patitas.mascota.domain.port.in.MascotaService;
import com.patitas.mascota.domain.port.out.MascotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MascotaServiceImpl implements MascotaService {

    private final MascotaRepository repository;

    @Override
    public List<Mascota> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Mascota> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Mascota> findByDuenoId(Long duenoId) {
        return repository.findByDuenoId(duenoId);
    }

    @Override
    public Mascota save(Mascota value) {
        value.setId(null);
        return repository.save(value);
    }

    @Override
    @Transactional
    public Mascota update(Long id, Mascota value) {
        Mascota stored = requireResource(id);
        stored.setNombre(value.getNombre());
        stored.setEspecie(value.getEspecie());
        stored.setRaza(value.getRaza());
        stored.setFechaNacimiento(value.getFechaNacimiento());
        stored.setEdad(value.getEdad());
        stored.setDuenoId(value.getDuenoId());
        return repository.save(stored);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        requireResource(id);
        repository.deleteById(id);
    }

    private Mascota requireResource(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada: " + id));
    }
}
