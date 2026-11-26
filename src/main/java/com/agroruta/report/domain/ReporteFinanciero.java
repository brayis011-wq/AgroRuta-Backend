package com.agroruta.report.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Costos, ingresos y ganancia de cada temporada: la base de los reportes de costos y de rentabilidad.
 * {@code gastos} son los gastos generales de las fincas en el mismo periodo (no son de ninguna temporada).
 */
public record ReporteFinanciero(LocalDateTime generatedAt, List<TemporadaFinanzas> temporadas, GastosGenerales gastos) {
    public ReporteFinanciero {
        temporadas = temporadas != null ? List.copyOf(temporadas) : List.of();
        gastos = gastos != null ? gastos : GastosGenerales.NINGUNO;
    }

    public ReporteFinanciero(LocalDateTime generatedAt, List<TemporadaFinanzas> temporadas) {
        this(generatedAt, temporadas, GastosGenerales.NINGUNO);
    }

    /** Solo las temporadas que estuvieron en el campo durante el periodo (con todos sus costos e ingresos) */
    public ReporteFinanciero en(Periodo periodo) {
        if (periodo == null || periodo.esTodo()) return this;
        return new ReporteFinanciero(
                generatedAt,
                temporadas.stream()
                        .filter(t -> t.siembra() != null
                                && periodo.seCruzaCon(
                                        t.siembra().fechaSiembra(), t.siembra().fechaFin()))
                        .toList(),
                gastos);
    }

    public ReporteFinanciero conGastos(GastosGenerales g) {
        return new ReporteFinanciero(generatedAt, temporadas, g);
    }
}
