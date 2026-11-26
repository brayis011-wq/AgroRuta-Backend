package com.agroruta.configuration.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        String telefono,

        @Size(max = 350_000, message = "La foto es muy pesada. Elige una imagen más pequeña.")
        String fotoPerfil) {}
