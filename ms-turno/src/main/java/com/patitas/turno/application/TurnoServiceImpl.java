package com.patitas.turno.application;

import com.patitas.turno.domain.exception.ResourceNotFoundException;
import com.patitas.turno.domain.model.Turno;
import com.patitas.turno.domain.port.in.TurnoService;
import com.patitas.turno.domain.port.out.TurnoRepository;
import com.patitas.turno.domain.model.EstadoTurno;
import com.patitas.turno.domain.port.out.MascotaServicePort;
import com.patitas.turno.domain.port.out.VeterinarioServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TurnoServiceImpl implements TurnoService {

    private final TurnoRepository repository;
    private final MascotaServicePort mascotaService;
    private final VeterinarioServicePort veterinarioService;

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
        value.setId(null);
        validateReferences(value);
        value.setEstado(EstadoTurno.PENDIENTE);
        return repository.save(value);
    }

    @Override
    @Transactional
    public Turno update(Long id, Turno value) {
        Turno stored = requireResource(id);
        validateReferences(value);
        stored.setFechaHora(value.getFechaHora());
        stored.setMascotaId(value.getMascotaId());
        stored.setVeterinarioId(value.getVeterinarioId());
        stored.setMotivo(value.getMotivo());
        return repository.save(stored);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        requireResource(id);
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public Turno cancelar(Long id) {
        Turno stored = requireResource(id);
        stored.setEstado(EstadoTurno.CANCELADO);
        return repository.save(stored);
    }

    private void validateReferences(Turno value) {
        if (!mascotaService.existsById(value.getMascotaId())) {
            throw new ResourceNotFoundException("Mascota no encontrada: " + value.getMascotaId());
        }
        if (!veterinarioService.existsById(value.getVeterinarioId())) {
            throw new ResourceNotFoundException("Veterinario no encontrado: " + value.getVeterinarioId());
        }
    }

    private Turno requireResource(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + id));
    }
}
