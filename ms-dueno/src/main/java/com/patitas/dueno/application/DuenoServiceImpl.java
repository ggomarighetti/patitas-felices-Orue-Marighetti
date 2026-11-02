package com.patitas.dueno.application;

import com.patitas.dueno.domain.exception.DuplicateResourceException;
import com.patitas.dueno.domain.exception.ResourceNotFoundException;
import com.patitas.dueno.domain.model.Dueno;
import com.patitas.dueno.domain.port.in.DuenoService;
import com.patitas.dueno.domain.port.out.DuenoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Locale;

@Service
public class DuenoServiceImpl implements DuenoService {

    private final DuenoRepository duenoRepository;

    public DuenoServiceImpl(DuenoRepository duenoRepository) {
        this.duenoRepository = duenoRepository;
    }

    @Override
    public List<Dueno> findAll() {
        return duenoRepository.findAll();
    }

    @Override
    public Optional<Dueno> findById(Long id) {
        return duenoRepository.findById(id);
    }

    @Override
    @Transactional
    public Dueno save(Dueno dueno) {
        dueno.setId(null);
        dueno.setEmail(normalizeEmail(dueno.getEmail()));
        if (duenoRepository.existsByEmail(dueno.getEmail())) {
            throw new DuplicateResourceException("Ya existe un dueño con el email: " + dueno.getEmail());
        }
        return duenoRepository.save(dueno);
    }

    @Override
    @Transactional
    public Dueno update(Long id, Dueno dueno) {
        requireOwner(id);
        dueno.setEmail(normalizeEmail(dueno.getEmail()));
        if (duenoRepository.existsByEmailAndIdNot(dueno.getEmail(), id)) {
            throw new DuplicateResourceException("Ya existe un dueño con el email: " + dueno.getEmail());
        }
        return duenoRepository.update(id, dueno);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        requireOwner(id);
        duenoRepository.deleteById(id);
    }

    private void requireOwner(Long id) {
        duenoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un dueño con id: " + id));
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
