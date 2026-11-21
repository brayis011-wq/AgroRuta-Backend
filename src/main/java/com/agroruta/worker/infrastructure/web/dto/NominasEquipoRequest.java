package com.agroruta.worker.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** La quincena de todo el equipo: un solo periodo para todos. */
public class NominasEquipoRequest {

    @NotNull(message = "La fecha de inicio es obligatoria")
    public LocalDate periodoInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    public LocalDate periodoFin;

    @JsonIgnore
    @AssertTrue(message = "La fecha de fin no puede ser anterior a la de inicio")
    public boolean isPeriodoValido() {
        return periodoInicio == null || periodoFin == null || !periodoFin.isBefore(periodoInicio);
    }
}
