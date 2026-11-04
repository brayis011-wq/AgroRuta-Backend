package com.agroruta.worker.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CargoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    public String nombre;

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    public String descripcion;

    @NotNull(message = "El valor del jornal es obligatorio")
    @Positive(message = "El valor del jornal debe ser mayor a 0")
    @DecimalMax(value = "100000000", message = "El valor del jornal es demasiado alto")
    @Digits(integer = 10, fraction = 2, message = "El valor del jornal admite máximo 2 decimales")
    public BigDecimal valorJornal;
}
