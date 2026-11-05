package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CosechaRequest {

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha de cosecha no puede ser futura")
    private LocalDate fecha;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Double cantidadKg;

    // El valor exacto (PRIMERA, SEGUNDA, TERCERA) lo valida CosechaService.
    @NotBlank(message = "La calidad es obligatoria")
    private String calidad;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;

    // Opcional: precio de venta por kilo (COP)
    @PositiveOrZero(message = "El precio no puede ser negativo")
    private Double precioKg;

    @NotNull(message = "La siembra es obligatoria")
    @Positive(message = "La siembra no es válida")
    private Long siembraId;
}
