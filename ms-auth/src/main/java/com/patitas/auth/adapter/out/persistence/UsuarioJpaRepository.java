package com.patitas.auth.adapter.out.persistence;

import com.patitas.auth.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
}
