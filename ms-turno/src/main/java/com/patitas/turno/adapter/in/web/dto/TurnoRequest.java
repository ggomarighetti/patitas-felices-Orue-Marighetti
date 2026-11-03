package com.patitas.turno.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoRequest {

    @NotNull
    private LocalDateTime fechaHora;

    @NotNull
    @Positive
    private Long mascotaId;

    @NotNull
    @Positive
    private Long veterinarioId;

    @NotBlank
    @Size(max = 255)
    private String motivo;
}
