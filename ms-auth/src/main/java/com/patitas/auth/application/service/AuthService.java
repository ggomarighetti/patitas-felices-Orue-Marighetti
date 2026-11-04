package com.patitas.auth.application.service;

import com.patitas.auth.application.dto.AuthResponse;
import com.patitas.auth.application.dto.LoginRequest;
import com.patitas.auth.application.dto.RegisterRequest;
import com.patitas.auth.domain.exception.DuplicateResourceException;
import com.patitas.auth.domain.exception.InvalidCredentialsException;
import com.patitas.auth.domain.exception.InvalidRequestException;
import com.patitas.auth.domain.model.Usuario;
import com.patitas.auth.domain.port.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validatePasswordLength(request.password());
        if (repository.findByUsername(request.username()).isPresent()) {
            throw new DuplicateResourceException("El nombre de usuario ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        Usuario saved = repository.save(usuario);
        return new AuthResponse(jwtService.generateToken(saved.getUsername(), saved.getRol()));
    }

    public AuthResponse login(LoginRequest request) {
        validatePasswordLength(request.password());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
            String rol = authentication.getAuthorities().stream().map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_")).findFirst().orElseThrow().substring(5);
            return new AuthResponse(jwtService.generateToken(authentication.getName(), rol));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos", exception);
        }
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("La contraseña no puede superar 72 bytes UTF-8");
        }
    }
}
