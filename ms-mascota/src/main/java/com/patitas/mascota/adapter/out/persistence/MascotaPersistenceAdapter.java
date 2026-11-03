package com.patitas.mascota.adapter.out.persistence;

import com.patitas.mascota.domain.model.Mascota;
import com.patitas.mascota.domain.port.out.MascotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MascotaPersistenceAdapter implements MascotaRepository {

    private final MascotaJpaRepository repository;

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
        return repository.saveAndFlush(value);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
