package com.agroruta.backup.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Confirmación explícita: el usuario debe escribir RESTAURAR. */
public record RestoreRequest(
        @NotBlank(message = "La confirmación es obligatoria")
        @Pattern(regexp = "RESTAURAR", message = "Debes escribir RESTAURAR para confirmar")
        String confirmacion
) {
}