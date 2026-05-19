package com.agroruta.configuration.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;

    private String fotoPerfil;

    public String getNombre()           { return nombre; }
    public void setNombre(String n)     { this.nombre = n; }
    public String getTelefono()         { return telefono; }
    public void setTelefono(String t)   { this.telefono = t; }
    public String getFotoPerfil()       { return fotoPerfil; }
    public void setFotoPerfil(String f) { this.fotoPerfil = f; }
}