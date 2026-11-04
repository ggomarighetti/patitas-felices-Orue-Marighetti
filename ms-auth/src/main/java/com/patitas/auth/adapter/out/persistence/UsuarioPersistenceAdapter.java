package com.patitas.auth.adapter.out.persistence;

import com.patitas.auth.domain.exception.DuplicateResourceException;
import com.patitas.auth.domain.model.Usuario;
import com.patitas.auth.domain.port.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository repository;

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    public Usuario save(Usuario usuario) {
        try {
            return repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            String detail = exception.getMostSpecificCause().getMessage();
            if (detail != null && detail.contains("uk_usuarios_username")) {
                throw new DuplicateResourceException("El nombre de usuario ya está registrado", exception);
            }
            throw exception;
        }
    }
}
