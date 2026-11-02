package com.patitas.dueno.adapter.out.persistence;

import com.patitas.dueno.domain.exception.DuplicateResourceException;
import com.patitas.dueno.domain.exception.ResourceNotFoundException;
import com.patitas.dueno.domain.model.Dueno;
import com.patitas.dueno.domain.port.out.DuenoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DuenoPersistenceAdapter implements DuenoRepository {

    private final DuenoJpaRepository jpaRepository;

    @Override
    public List<Dueno> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<Dueno> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Dueno save(Dueno dueno) {
        try {
            return jpaRepository.saveAndFlush(dueno);
        } catch (DataIntegrityViolationException exception) {
            String detail = exception.getMostSpecificCause().getMessage();
            if (detail != null && detail.contains("uk_duenos_email")) {
                throw new DuplicateResourceException("Ya existe un dueño con el email: " + dueno.getEmail(), exception);
            }
            throw exception;
        }
    }

    @Override
    public Dueno update(Long id, Dueno dueno) {
        Dueno stored = jpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un dueño con id: " + id));
        stored.setNombre(dueno.getNombre());
        stored.setApellido(dueno.getApellido());
        stored.setEmail(dueno.getEmail());
        stored.setTelefono(dueno.getTelefono());
        return save(stored);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return jpaRepository.existsByEmailAndIdNot(email, id);
    }
}
