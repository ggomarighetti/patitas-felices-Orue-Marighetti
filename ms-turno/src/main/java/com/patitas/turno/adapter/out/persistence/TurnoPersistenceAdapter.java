package com.patitas.turno.adapter.out.persistence;

import com.patitas.turno.domain.model.Turno;
import com.patitas.turno.domain.port.out.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TurnoPersistenceAdapter implements TurnoRepository {

    private final TurnoJpaRepository repository;

    @Override
    public List<Turno> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Turno> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Turno> findByVeterinarioId(Long veterinarioId) {
        return repository.findByVeterinarioId(veterinarioId);
    }

    @Override
    public List<Turno> findByMascotaId(Long mascotaId) {
        return repository.findByMascotaId(mascotaId);
    }

    @Override
    public Turno save(Turno value) {
        return repository.saveAndFlush(value);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
