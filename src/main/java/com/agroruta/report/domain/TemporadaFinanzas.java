package com.agroruta.report.domain;

/** Una temporada con sus costos; {@code costos} es null si no se pudieron calcular. */
public record TemporadaFinanzas(CropDetail siembra, CostosTemporada costos) {}
