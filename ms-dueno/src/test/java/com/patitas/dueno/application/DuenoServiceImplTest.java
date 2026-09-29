package com.patitas.dueno.application;

import com.patitas.dueno.domain.exception.DuplicatedEmailException;
import com.patitas.dueno.domain.model.Dueno;
import com.patitas.dueno.domain.port.in.DuenoService;
import com.patitas.dueno.domain.port.out.DuenoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuenoServiceImplTest {

    private InMemoryDuenoRepository repository;
    private DuenoService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDuenoRepository();
        service = new DuenoServiceImpl(repository);
    }

    @Test
    void findAllReturnsAnEmptyListWhenThereAreNoOwners() {
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void findAllReturnsTheStoredOwners() {
        Dueno ana = repository.save(dueno("ana@example.com"));
        Dueno luis = repository.save(dueno("luis@example.com"));

        assertEquals(List.of(ana, luis), service.findAll());
    }

    @Test
    void findByIdReturnsTheRequestedOwner() {
        repository.save(dueno("ana@example.com"));
        Dueno luis = repository.save(dueno("luis@example.com"));

        assertEquals(Optional.of(luis), service.findById(luis.getId()));
    }

    @Test
    void findByIdReturnsEmptyForAnUnknownId() {
        assertTrue(service.findById(999L).isEmpty());
    }

    @Test
    void saveReturnsTheOwnerWithTheIdAssignedByTheRepository() {
        Dueno input = dueno("ana@example.com");

        Dueno saved = service.save(input);

        assertNotNull(saved.getId());
        assertEquals(input.getEmail(), saved.getEmail());
        assertEquals(Optional.of(saved), repository.findById(saved.getId()));
        assertEquals(1, repository.saveCalls);
    }

    @Test
    void saveRejectsAnExistingEmailWithoutWritingToTheRepository() {
        Dueno original = repository.save(dueno("ana@example.com"));
        Dueno duplicate = new Dueno(null, "Otra", "Persona", original.getEmail(), "1122222222");
        int previousSaveCalls = repository.saveCalls;

        DuplicatedEmailException error = assertThrows(
                DuplicatedEmailException.class, () -> service.save(duplicate));

        assertTrue(error.getMessage().contains(original.getEmail()));
        assertEquals(previousSaveCalls, repository.saveCalls);
        assertEquals(List.of(original), repository.findAll());
    }

    @Test
    void saveAllowsAnotherOwnerWithADifferentEmail() {
        Dueno first = repository.save(dueno("ana@example.com"));

        Dueno second = service.save(dueno("luis@example.com"));

        assertFalse(first.getId().equals(second.getId()));
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void updateDelegatesTheRequestedIdAndData() {
        Dueno original = repository.save(dueno("ana@example.com"));
        Dueno untouched = repository.save(dueno("luis@example.com"));
        Dueno changes = new Dueno(null, "Ana María", "Gómez", "ana.nueva@example.com", "1199999999");

        Dueno updated = service.update(original.getId(), changes);

        assertEquals(original.getId(), updated.getId());
        assertEquals(changes.getNombre(), updated.getNombre());
        assertEquals(changes.getApellido(), updated.getApellido());
        assertEquals(changes.getEmail(), updated.getEmail());
        assertEquals(changes.getTelefono(), updated.getTelefono());
        assertEquals(Optional.of(updated), repository.findById(original.getId()));
        assertEquals(Optional.of(untouched), repository.findById(untouched.getId()));
    }

    @Test
    void deleteByIdRemovesOnlyTheRequestedOwner() {
        Dueno deleted = repository.save(dueno("ana@example.com"));
        Dueno remaining = repository.save(dueno("luis@example.com"));

        service.deleteById(deleted.getId());

        assertTrue(repository.findById(deleted.getId()).isEmpty());
        assertEquals(List.of(remaining), repository.findAll());
    }

    @Test
    void savePropagatesRepositoryFailures() {
        RuntimeException failure = new IllegalStateException("Persistencia no disponible");
        DuenoService failingService = new DuenoServiceImpl(new InMemoryDuenoRepository() {
            @Override
            public Dueno save(Dueno dueno) {
                throw failure;
            }
        });

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> failingService.save(dueno("ana@example.com")));

        assertSame(failure, thrown);
    }

    private static Dueno dueno(String email) {
        return new Dueno(null, "Ana", "Gómez", email, "1112345678");
    }

    // Doble de prueba del puerto de salida; no es un adaptador de producción.
    private static class InMemoryDuenoRepository implements DuenoRepository {

        private final Map<Long, Dueno> owners = new LinkedHashMap<>();
        private long nextId = 1;
        private int saveCalls;

        @Override
        public List<Dueno> findAll() {
            return List.copyOf(owners.values());
        }

        @Override
        public Optional<Dueno> findById(Long id) {
            return Optional.ofNullable(owners.get(id));
        }

        @Override
        public Dueno save(Dueno dueno) {
            saveCalls++;
            Dueno stored = copyWithId(nextId++, dueno);
            owners.put(stored.getId(), stored);
            return stored;
        }

        @Override
        public Dueno update(Long id, Dueno dueno) {
            if (!owners.containsKey(id)) {
                throw new NoSuchElementException("Dueño inexistente: " + id);
            }
            Dueno updated = copyWithId(id, dueno);
            owners.put(id, updated);
            return updated;
        }

        @Override
        public void deleteById(Long id) {
            owners.remove(id);
        }

        @Override
        public boolean existsByEmail(String email) {
            return owners.values().stream().anyMatch(dueno -> Objects.equals(dueno.getEmail(), email));
        }

        private static Dueno copyWithId(Long id, Dueno dueno) {
            return new Dueno(id, dueno.getNombre(), dueno.getApellido(), dueno.getEmail(), dueno.getTelefono());
        }
    }
}
