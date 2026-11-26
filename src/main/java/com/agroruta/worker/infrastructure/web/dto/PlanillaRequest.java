package com.agroruta.worker.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Varios jornales a la vez desde la planilla semanal: mismo cultivo y mismas labores. */
public class PlanillaRequest {

    @NotNull(message = "El cultivo es obligatorio")
    @Positive(message = "El cultivo no es válido")
    public Long cultivoId;

    @NotEmpty(message = "Selecciona al menos una labor")
    @Size(max = 50, message = "Demasiadas labores")
    public List<@NotNull(message = "Labor no válida") Long> actividadIds;

    @NotEmpty(message = "No hay jornales para anotar")
    @Size(max = 200, message = "Demasiados jornales a la vez")
    public List<@Valid Entrada> entradas;

    public static class Entrada {
        @NotNull(message = "El trabajador es obligatorio")
        public Long trabajadorId;

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "No se puede registrar un jornal en fecha futura")
        public LocalDate fecha;

        @Positive(message = "Los kilos deben ser más de 0")
        @DecimalMax(value = "2000", message = "Son demasiados kilos para un día")
        public BigDecimal kilos;
    }
}
