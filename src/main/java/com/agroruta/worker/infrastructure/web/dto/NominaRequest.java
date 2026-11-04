package com.agroruta.worker.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class NominaRequest {

    @NotNull(message = "El trabajador es obligatorio")
    @Positive(message = "El trabajador no es válido")
    public Long trabajadorId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    public LocalDate periodoInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    public LocalDate periodoFin;

    /** Si falta alguna fecha, ya lo reporta @NotNull; aquí solo se valida el orden. */
    @JsonIgnore
    @AssertTrue(message = "La fecha de fin no puede ser anterior a la de inicio")
    public boolean isPeriodoValido() {
        return periodoInicio == null || periodoFin == null || !periodoFin.isBefore(periodoInicio);
    }
}
