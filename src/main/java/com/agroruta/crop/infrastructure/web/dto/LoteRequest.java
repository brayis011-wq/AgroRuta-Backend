package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @NotNull(message = "El área es obligatoria")
    @Positive(message = "El área debe ser mayor a 0")
    private Double area;

    @NotNull(message = "La finca es obligatoria")
    @Positive(message = "La finca no es válida")
    private Long fincaId;
}
