package com.agroruta.worker.infrastructure.web.dto;

import java.time.LocalDate;

public class NominaRequest {
    public Long trabajadorId;
    public LocalDate periodoInicio;
    public LocalDate periodoFin;
}