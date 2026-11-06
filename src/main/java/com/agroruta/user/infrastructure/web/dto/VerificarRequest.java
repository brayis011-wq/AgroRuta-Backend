package com.agroruta.user.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificarRequest(
        @NotBlank(message = "El enlace no es válido")
        @Size(max = 100, message = "El enlace no es válido")
        String token) {}
