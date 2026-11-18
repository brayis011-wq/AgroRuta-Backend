package com.agroruta.report.domain;

import java.time.LocalDateTime;
import java.util.List;

/** Costos, ingresos y ganancia de cada temporada: la base de los reportes de costos y de rentabilidad. */
public record ReporteFinanciero(LocalDateTime generatedAt, List<TemporadaFinanzas> temporadas) {
    public ReporteFinanciero {
        temporadas = temporadas != null ? List.copyOf(temporadas) : List.of();
    }
}
