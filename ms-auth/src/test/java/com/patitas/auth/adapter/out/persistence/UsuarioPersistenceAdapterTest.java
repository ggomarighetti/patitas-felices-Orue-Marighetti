package com.patitas.auth.adapter.out.persistence;

import com.patitas.auth.domain.exception.DuplicateResourceException;
import com.patitas.auth.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioPersistenceAdapterTest {

    @Test
    void concurrentUsernameConflictPreservesItsOriginalCause() {
        UsuarioJpaRepository repository = mock(UsuarioJpaRepository.class);
        Usuario usuario = new Usuario();
        DataIntegrityViolationException original = new DataIntegrityViolationException("uk_usuarios_username");
        when(repository.saveAndFlush(usuario)).thenThrow(original);

        DuplicateResourceException translated = assertThrows(DuplicateResourceException.class,
                () -> new UsuarioPersistenceAdapter(repository).save(usuario));

        assertSame(original, translated.getCause());
    }

    @Test
    void unrelatedPersistenceFailureIsNotReportedAsADuplicateUsername() {
        UsuarioJpaRepository repository = mock(UsuarioJpaRepository.class);
        Usuario usuario = new Usuario();
        DataIntegrityViolationException original = new DataIntegrityViolationException("otro problema");
        when(repository.saveAndFlush(usuario)).thenThrow(original);

        assertSame(original, assertThrows(DataIntegrityViolationException.class,
                () -> new UsuarioPersistenceAdapter(repository).save(usuario)));
    }
}
