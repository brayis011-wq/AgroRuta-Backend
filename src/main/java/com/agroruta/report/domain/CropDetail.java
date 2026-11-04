package com.agroruta.report.domain;

import java.time.LocalDate;

public record CropDetail(
        Long siembraId,
        String variedad,           // "Cultivo · Variedad", p. ej. "Café · Castillo"
        String estado,             // EstadoCultivo enum como string
        LocalDate fechaSiembra,    // fecha de siembra, no hay fecha de inicio de etapa
        long diasDesdeSiembra,     // días desde que se sembró
        Double totalKgCosechado,
        int totalCosechas
) {}