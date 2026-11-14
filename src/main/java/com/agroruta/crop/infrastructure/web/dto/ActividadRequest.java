package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.math.BigDecimal;

@Data
public class ActividadRequest {

    // El valor exacto (PODA, TUTOREO, ...) lo valida ActividadCultivoService.
    @NotBlank(message = "El tipo es obligatorio")
    private String tipo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;

    // Sin @PastOrPresent: una actividad puede quedar programada.
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    // Opcional: materiales o servicios de la actividad (COP), aparte de los jornales
    @PositiveOrZero(message = "El costo no puede ser negativo")
    @DecimalMax(value = "999999999999.99", message = "El valor es demasiado grande")
    private BigDecimal costo;

    @NotNull(message = "La siembra es obligatoria")
    @Positive(message = "La siembra no es válida")
    private Long siembraId;
}
