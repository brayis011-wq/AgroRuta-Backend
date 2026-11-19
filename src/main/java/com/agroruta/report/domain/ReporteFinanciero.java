package com.agroruta.report.domain;

import java.time.LocalDateTime;
import java.util.List;

/** Costos, ingresos y ganancia de cada temporada: la base de los reportes de costos y de rentabilidad. */
public record ReporteFinanciero(LocalDateTime generatedAt, List<TemporadaFinanzas> temporadas) {
    public ReporteFinanciero {
        temporadas = temporadas != null ? List.copyOf(temporadas) : List.of();
    }

    /** Solo las temporadas que estuvieron en el campo durante el periodo (con todos sus costos e ingresos) */
    public ReporteFinanciero en(Periodo periodo) {
        if (periodo == null || periodo.esTodo()) return this;
        return new ReporteFinanciero(generatedAt, temporadas.stream()
                .filter(t -> t.siembra() != null && periodo.seCruzaCon(t.siembra().fechaSiembra(), t.siembra().fechaFin()))
                .toList());
    }
}
