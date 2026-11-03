package com.patitas.mascota.adapter.in.web;

import com.patitas.mascota.adapter.in.web.dto.MascotaMapper;
import com.patitas.mascota.adapter.in.web.dto.MascotaRequest;
import com.patitas.mascota.adapter.in.web.dto.MascotaResponse;
import com.patitas.mascota.domain.exception.ResourceNotFoundException;
import com.patitas.mascota.domain.port.in.MascotaService;
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

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
@Tag(name = "mascotas")
public class MascotaController {

    private final MascotaService service;

    @GetMapping
    public List<MascotaResponse> findAll() {
        return service.findAll().stream().map(MascotaMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MascotaResponse findById(@PathVariable @Positive Long id) {
        return service.findById(id).map(MascotaMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada: " + id));
    }

    @GetMapping("/dueno/{duenoId}")
    public List<MascotaResponse> findByDuenoId(@PathVariable @Positive Long duenoId) {
        return service.findByDuenoId(duenoId).stream().map(MascotaMapper::toResponse).toList();
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    public ResponseEntity<MascotaResponse> save(@Valid @RequestBody MascotaRequest request) {
        MascotaResponse response = MascotaMapper.toResponse(service.save(MascotaMapper.toEntity(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public MascotaResponse update(@PathVariable @Positive Long id, @Valid @RequestBody MascotaRequest request) {
        return MascotaMapper.toResponse(service.update(id, MascotaMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado", content = @Content)
    public ResponseEntity<Void> deleteById(@PathVariable @Positive Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
