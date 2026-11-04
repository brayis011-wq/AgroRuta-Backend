package com.agroruta.user.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    private String email;

    // BCrypt solo usa los primeros 72 bytes, por eso el máximo.
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;

    // Opcional: AuthService decide qué roles se permiten en el autorregistro.
    @Size(max = 20, message = "Rol no válido")
    private String rol;

    // Obligatoria solo para el rol Trabajador (lo valida AuthService).
    @Pattern(regexp = "^$|^[0-9A-Za-z.\\- ]{5,20}$",
             message = "La cédula debe tener entre 5 y 20 caracteres (números, letras, puntos o guiones)")
    private String cedula;
}
