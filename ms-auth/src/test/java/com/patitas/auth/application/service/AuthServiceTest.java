package com.patitas.auth.application.service;

import com.patitas.auth.application.dto.LoginRequest;
import com.patitas.auth.application.dto.RegisterRequest;
import com.patitas.auth.config.SecurityConfig;
import com.patitas.auth.domain.exception.DuplicateResourceException;
import com.patitas.auth.domain.exception.InvalidCredentialsException;
import com.patitas.auth.domain.exception.InvalidRequestException;
import com.patitas.auth.domain.model.Usuario;
import com.patitas.auth.domain.port.out.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UsuarioRepository repository;
    private PasswordEncoder encoder;
    private JwtService jwt;
    private AuthService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        SecurityConfig config = new SecurityConfig();
        encoder = config.passwordEncoder();
        jwt = new JwtService("test-only-auth-key-0123456789abcdef0123456789", 86400000);
        service = new AuthService(repository, encoder,
                config.authenticationManager(config.userDetailsService(repository), encoder), jwt);
    }

    @Test
    void registrationStoresBcryptAndReturnsOnlyAUsableToken() {
        when(repository.findByUsername("jorge")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(new RegisterRequest("jorge", "1234", "ADMIN"));

        ArgumentCaptor<Usuario> stored = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(stored.capture());
        assertNotEquals("1234", stored.getValue().getPassword());
        assertTrue(encoder.matches("1234", stored.getValue().getPassword()));
        assertEquals("ADMIN", stored.getValue().getRol());
        assertTrue(jwt.validateToken(response.token()));
        assertEquals("jorge", jwt.extractUsername(response.token()));
    }

    @Test
    void duplicateUsernameDoesNotCreateASecondUser() {
        when(repository.findByUsername("jorge")).thenReturn(Optional.of(usuario()));

        assertThrows(DuplicateResourceException.class,
                () -> service.register(new RegisterRequest("jorge", "1234", "ADMIN")));

        verify(repository, never()).save(any());
    }

    @Test
    void loginChecksRealBcryptThroughTheAuthenticationManager() {
        when(repository.findByUsername("jorge")).thenReturn(Optional.of(usuario()));

        var response = service.login(new LoginRequest("jorge", "1234"));

        assertTrue(jwt.validateToken(response.token()));
        assertEquals("jorge", jwt.extractUsername(response.token()));
        verify(repository, never()).save(any());
    }

    @Test
    void unknownUserAndIncorrectPasswordHaveTheSamePublicError() {
        when(repository.findByUsername("jorge")).thenReturn(Optional.of(usuario()));
        when(repository.findByUsername("nadie")).thenReturn(Optional.empty());

        var wrong = assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("jorge", "incorrecta")));
        var missing = assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("nadie", "incorrecta")));

        assertEquals(wrong.getMessage(), missing.getMessage());
        assertInstanceOf(BadCredentialsException.class, wrong.getCause());
        assertInstanceOf(BadCredentialsException.class, missing.getCause());
    }

    @Test
    void unicodePasswordCannotExceedTheBcryptByteLimit() {
        assertThrows(InvalidRequestException.class,
                () -> service.register(new RegisterRequest("jorge", "é".repeat(40), "ADMIN")));

        verifyNoInteractions(repository);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setUsername("jorge");
        usuario.setPassword(encoder.encode("1234"));
        usuario.setRol("ADMIN");
        return usuario;
    }
}
