package com.patitas.veterinario.adapter.in.web;

import com.patitas.veterinario.adapter.in.web.dto.VeterinarioMapper;
import com.patitas.veterinario.adapter.in.web.dto.VeterinarioRequest;
import com.patitas.veterinario.adapter.in.web.dto.VeterinarioResponse;
import com.patitas.veterinario.domain.exception.ResourceNotFoundException;
import com.patitas.veterinario.domain.port.in.VeterinarioService;
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
@RequestMapping("/api/veterinarios")
@RequiredArgsConstructor
@Tag(name = "veterinarios")
public class VeterinarioController {

    private final VeterinarioService service;

    @GetMapping
    public List<VeterinarioResponse> findAll() {
        return service.findAll().stream().map(VeterinarioMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public VeterinarioResponse findById(@PathVariable @Positive Long id) {
        return service.findById(id).map(VeterinarioMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado: " + id));
    }

    @GetMapping("/especialidad/{especialidad}")
    public List<VeterinarioResponse> findByEspecialidad(@PathVariable String especialidad) {
        return service.findByEspecialidad(especialidad).stream().map(VeterinarioMapper::toResponse).toList();
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    public ResponseEntity<VeterinarioResponse> save(@Valid @RequestBody VeterinarioRequest request) {
        VeterinarioResponse response = VeterinarioMapper.toResponse(service.save(VeterinarioMapper.toEntity(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public VeterinarioResponse update(@PathVariable @Positive Long id, @Valid @RequestBody VeterinarioRequest request) {
        return VeterinarioMapper.toResponse(service.update(id, VeterinarioMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado", content = @Content)
    public ResponseEntity<Void> deleteById(@PathVariable @Positive Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
