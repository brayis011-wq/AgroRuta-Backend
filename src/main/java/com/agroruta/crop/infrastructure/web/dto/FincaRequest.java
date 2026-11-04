package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FincaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 255, message = "La ubicación no puede superar 255 caracteres")
    private String ubicacion;

    @NotNull(message = "Las hectáreas son obligatorias")
    @Positive(message = "Las hectáreas deben ser mayores a 0")
    @DecimalMax(value = "100000", message = "Las hectáreas no pueden superar 100000")
    private Double hectareas;

    @NotNull(message = "El agricultor es obligatorio")
    @Positive(message = "El agricultor no es válido")
    private Long agricultorId;
}
