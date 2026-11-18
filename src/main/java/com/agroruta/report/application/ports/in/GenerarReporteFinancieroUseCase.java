package com.agroruta.report.application.ports.in;

import com.agroruta.report.domain.ReporteFinanciero;

public interface GenerarReporteFinancieroUseCase {

    /** Costos e ingresos de todas las temporadas (uso del Administrador). */
    ReporteFinanciero generate();

    /** Solo las temporadas de un agricultor. Si agricultorId es null, el reporte sale vacío. */
    ReporteFinanciero generateFor(Long agricultorId);
}
