package com.patitas.turno.application;

import com.patitas.turno.domain.exception.RemoteServiceUnavailableException;
import com.patitas.turno.domain.exception.ResourceNotFoundException;
import com.patitas.turno.domain.model.EstadoTurno;
import com.patitas.turno.domain.model.Turno;
import com.patitas.turno.domain.port.out.MascotaServicePort;
import com.patitas.turno.domain.port.out.TurnoRepository;
import com.patitas.turno.domain.port.out.VeterinarioServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TurnoServiceImplTest {

    private TurnoRepository repository;
    private MascotaServicePort mascotas;
    private VeterinarioServicePort veterinarios;
    private TurnoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(TurnoRepository.class);
        mascotas = mock(MascotaServicePort.class);
        veterinarios = mock(VeterinarioServicePort.class);
        service = new TurnoServiceImpl(repository, mascotas, veterinarios);
    }

    @Test
    void validatesBothReferencesBeforePersistingAPendingTurno() {
        Turno input = turno();
        input.setId(999L);
        input.setEstado(EstadoTurno.CONFIRMADO);
        when(mascotas.existsById(1L)).thenReturn(true);
        when(veterinarios.existsById(2L)).thenReturn(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Turno result = service.save(input);

        assertNull(result.getId());
        assertEquals(EstadoTurno.PENDIENTE, result.getEstado());
        InOrder order = inOrder(mascotas, veterinarios, repository);
        order.verify(mascotas).existsById(1L);
        order.verify(veterinarios).existsById(2L);
        order.verify(repository).save(input);
    }

    @Test
    void missingMascotaStopsValidationAndPersistence() {
        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class, () -> service.save(turno()));

        assertTrue(error.getMessage().contains("Mascota"));
        verifyNoInteractions(veterinarios, repository);
    }

    @Test
    void missingVeterinarioPreventsPersistence() {
        when(mascotas.existsById(1L)).thenReturn(true);

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class, () -> service.save(turno()));

        assertTrue(error.getMessage().contains("Veterinario"));
        verifyNoInteractions(repository);
    }

    @Test
    void unavailableMascotaPreservesTheErrorWithoutWriting() {
        RemoteServiceUnavailableException failure = new RemoteServiceUnavailableException("Servicio no disponible");
        when(mascotas.existsById(1L)).thenThrow(failure);

        assertSame(failure, assertThrows(RemoteServiceUnavailableException.class, () -> service.save(turno())));
        verifyNoInteractions(veterinarios, repository);
    }

    @Test
    void unavailableVeterinarioPreventsPersistence() {
        when(mascotas.existsById(1L)).thenReturn(true);
        when(veterinarios.existsById(2L)).thenThrow(new RemoteServiceUnavailableException("Servicio no disponible"));

        assertThrows(RemoteServiceUnavailableException.class, () -> service.save(turno()));
        verifyNoInteractions(repository);
    }

    @Test
    void invalidUpdateDoesNotMutateTheStoredTurno() {
        Turno stored = turno();
        stored.setId(3L);
        when(repository.findById(3L)).thenReturn(Optional.of(stored));
        Turno changes = turno();
        changes.setMascotaId(999L);
        changes.setMotivo("No debe guardarse");

        assertThrows(ResourceNotFoundException.class, () -> service.update(3L, changes));

        assertEquals(1L, stored.getMascotaId());
        assertEquals("Control", stored.getMotivo());
        verify(repository, never()).save(any());
    }

    @Test
    void cancellationDoesNotDependOnRemoteServices() {
        Turno stored = turno();
        when(repository.findById(3L)).thenReturn(Optional.of(stored));
        when(repository.save(stored)).thenReturn(stored);

        assertEquals(EstadoTurno.CANCELADO, service.cancelar(3L).getEstado());
        verifyNoInteractions(mascotas, veterinarios);
    }

    @Test
    void missingTurnoCannotBeCancelled() {
        assertThrows(ResourceNotFoundException.class, () -> service.cancelar(999L));
        verifyNoInteractions(mascotas, veterinarios);
        verify(repository, never()).save(any());
    }

    private Turno turno() {
        return new Turno(null, LocalDateTime.of(2026, 11, 15, 10, 0), 1L, 2L, "Control", EstadoTurno.PENDIENTE);
    }
}
