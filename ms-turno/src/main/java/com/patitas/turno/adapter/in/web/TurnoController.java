package com.patitas.turno.adapter.in.web;

import com.patitas.turno.adapter.in.web.dto.TurnoMapper;
import com.patitas.turno.adapter.in.web.dto.TurnoRequest;
import com.patitas.turno.adapter.in.web.dto.TurnoResponse;
import com.patitas.turno.domain.exception.ResourceNotFoundException;
import com.patitas.turno.domain.port.in.TurnoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@Tag(name = "turnos")
public class TurnoController {

    private final TurnoService service;

    @GetMapping
    public List<TurnoResponse> findAll() {
        return service.findAll().stream().map(TurnoMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TurnoResponse findById(@PathVariable @Positive Long id) {
        return service.findById(id).map(TurnoMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + id));
    }

    @GetMapping("/veterinario/{veterinarioId}")
    public List<TurnoResponse> findByVeterinarioId(@PathVariable @Positive Long veterinarioId) {
        return service.findByVeterinarioId(veterinarioId).stream().map(TurnoMapper::toResponse).toList();
    }

    @GetMapping("/mascota/{mascotaId}")
    public List<TurnoResponse> findByMascotaId(@PathVariable @Positive Long mascotaId) {
        return service.findByMascotaId(mascotaId).stream().map(TurnoMapper::toResponse).toList();
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    public ResponseEntity<TurnoResponse> save(@Valid @RequestBody TurnoRequest request) {
        TurnoResponse response = TurnoMapper.toResponse(service.save(TurnoMapper.toEntity(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public TurnoResponse update(@PathVariable @Positive Long id, @Valid @RequestBody TurnoRequest request) {
        return TurnoMapper.toResponse(service.update(id, TurnoMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado", content = @Content)
    public ResponseEntity<Void> deleteById(@PathVariable @Positive Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancelar")
    public TurnoResponse cancelar(@PathVariable @Positive Long id) {
        return TurnoMapper.toResponse(service.cancelar(id));
    }
}
