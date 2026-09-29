package com.patitas.dueno.application;

import com.patitas.dueno.domain.exception.DuplicatedEmailException;
import com.patitas.dueno.domain.model.Dueno;
import com.patitas.dueno.domain.port.in.DuenoService;
import com.patitas.dueno.domain.port.out.DuenoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
    public Dueno save(Dueno dueno) {
        if (duenoRepository.existsByEmail(dueno.getEmail())) {
            throw new DuplicatedEmailException(dueno.getEmail());
        }
        return duenoRepository.save(dueno);
    }

    @Override
    public Dueno update(Long id, Dueno dueno) {
        return duenoRepository.update(id, dueno);
    }

    @Override
    public void deleteById(Long id) {
        duenoRepository.deleteById(id);
    }
}
