package com.agroruta.notification.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerRequest(
        @NotBlank(message = "El enlace no es válido")
        @Size(max = 100, message = "El enlace no es válido")
        String token,
        // BCrypt solo usa los primeros 72 bytes, igual que en el registro
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password) {}
