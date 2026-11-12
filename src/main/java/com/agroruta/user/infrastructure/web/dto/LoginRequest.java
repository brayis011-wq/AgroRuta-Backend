package com.agroruta.user.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    private String email;

    // Sin mínimo de longitud: no se debe bloquear a usuarios con contraseñas antiguas.
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contraseña no puede superar 72 caracteres")
    private String password;

    /** Código de la app de autenticación o de respaldo; solo si la cuenta tiene verificación en dos pasos */
    @Size(max = 20, message = "El código no puede superar 20 caracteres")
    private String codigo;
}
