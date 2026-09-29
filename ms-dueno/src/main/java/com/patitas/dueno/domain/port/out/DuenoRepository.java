package com.patitas.dueno.domain.port.out;

import com.patitas.dueno.domain.model.Dueno;

import java.util.List;
import java.util.Optional;

public interface DuenoRepository {

    List<Dueno> findAll();

    Optional<Dueno> findById(Long id);

    Dueno save(Dueno dueno);

    Dueno update(Long id, Dueno dueno);

    void deleteById(Long id);

    boolean existsByEmail(String email);
}
