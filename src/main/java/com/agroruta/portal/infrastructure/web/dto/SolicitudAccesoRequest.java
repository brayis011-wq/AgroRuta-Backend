package com.agroruta.portal.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitudAccesoRequest(
        @NotBlank(message = "La cédula es obligatoria")
        @Pattern(regexp = "^[0-9A-Za-z.\\- ]{5,20}$",
                 message = "La cédula debe tener entre 5 y 20 caracteres (números, letras, puntos o guiones)")
        String cedula
) {}
