package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SiembraRequest {

    @NotNull(message = "La fecha de siembra es obligatoria")
    @PastOrPresent(message = "La fecha de siembra no puede ser futura")
    private LocalDate fechaSiembra;

    @NotNull(message = "La cantidad de plantas es obligatoria")
    @Positive(message = "La cantidad de plantas debe ser mayor a 0")
    @Max(value = 1_000_000, message = "La cantidad de plantas no puede superar 1.000.000")
    private Integer cantidadPlantas;

    // El valor exacto (COLOMBIA, GIGANTE, KENYA) lo valida SiembraService.
    @NotBlank(message = "La variedad es obligatoria")
    private String variedad;

    @NotNull(message = "El lote es obligatorio")
    @Positive(message = "El lote no es válido")
    private Long loteId;
}
