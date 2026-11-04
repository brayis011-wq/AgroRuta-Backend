package com.agroruta.admin.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarRolUsuarioRequest(
        @NotBlank(message = "El rol es obligatorio")
        @Size(max = 20, message = "Rol no válido")
        String rol
) {}
