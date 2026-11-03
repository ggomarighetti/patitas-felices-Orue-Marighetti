package com.patitas.mascota.adapter.in.web.dto;

import java.time.LocalDateTime;

public record ErrorResponse(LocalDateTime timestamp, int status, String error, String mensaje, String path) {
}
