package com.agroruta.worker.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ActividadRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    public String nombre;

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    public String descripcion;
}
