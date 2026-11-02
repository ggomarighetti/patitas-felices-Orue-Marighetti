package com.patitas.dueno.adapter.in.web;

import com.patitas.dueno.adapter.in.web.dto.DuenoRequest;
import com.patitas.dueno.adapter.in.web.dto.DuenoResponse;
import com.patitas.dueno.adapter.in.web.dto.ErrorResponse;
import com.patitas.dueno.domain.exception.ResourceNotFoundException;
import com.patitas.dueno.domain.port.in.DuenoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/duenos")
@RequiredArgsConstructor
@Tag(name = "Dueños", description = "CRUD de dueños de Patitas Felices")
public class DuenoController {

    private final DuenoService duenoService;
    private final DuenoWebMapper mapper;

    @GetMapping
    @Operation(summary = "Listar dueños")
    public List<DuenoResponse> findAll() {
        return duenoService.findAll().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un dueño")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Dueño encontrado"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Dueño inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public DuenoResponse findById(@PathVariable @Positive Long id) {
        return duenoService.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un dueño con id: " + id));
    }

    @PostMapping
    @Operation(summary = "Registrar un dueño")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Dueño registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email ya registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<DuenoResponse> save(@Valid @RequestBody DuenoRequest request) {
        DuenoResponse response = mapper.toResponse(duenoService.save(mapper.toDomain(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un dueño")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Dueño actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Dueño inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email ya registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public DuenoResponse update(@PathVariable @Positive Long id, @Valid @RequestBody DuenoRequest request) {
        return mapper.toResponse(duenoService.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un dueño")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Dueño eliminado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Identificador inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Dueño inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<Void> deleteById(@PathVariable @Positive Long id) {
        duenoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
