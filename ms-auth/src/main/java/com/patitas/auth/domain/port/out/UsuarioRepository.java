package com.patitas.auth.domain.port.out;

import com.patitas.auth.domain.model.Usuario;
import java.util.Optional;

public interface UsuarioRepository {

    Optional<Usuario> findByUsername(String username);

    Usuario save(Usuario usuario);
}
