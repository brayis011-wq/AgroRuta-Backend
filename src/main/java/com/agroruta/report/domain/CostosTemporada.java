package com.agroruta.report.domain;

/**
 * Lo que costó y lo que dejó una temporada, para los reportes (pesos colombianos).
 * Es el resumen de los costos de la siembra: sin el detalle por trabajador ni por mes.
 */
public record CostosTemporada(
        double manoObra,
        int jornales,
        double insumos,
        double otros,
        double costoTotal,
        double ingresos,
        double kgTotal,
        /** Kilos de cosechas que tienen precio de venta */
        double kgConPrecio,
        /** Ingresos − costos; null si ninguna cosecha tiene precio */
        Double ganancia,
        /** Costo total ÷ kilos cosechados; null sin cosechas */
        Double costoPorKg) {}
