package com.agroruta.configuration.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La nueva contraseña debe tener mínimo 8 caracteres")
    private String nuevaPassword;

    public String getPasswordActual()       { return passwordActual; }
    public void setPasswordActual(String p) { this.passwordActual = p; }
    public String getNuevaPassword()        { return nuevaPassword; }
    public void setNuevaPassword(String p)  { this.nuevaPassword = p; }
}