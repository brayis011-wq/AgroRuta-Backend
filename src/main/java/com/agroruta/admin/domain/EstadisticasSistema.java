package com.agroruta.admin.domain;

import java.math.BigDecimal;

/** Totales de cultivos y trabajadores de todo el sistema. */
public record EstadisticasSistema(
        long fincas,
        double hectareas,
        long lotes,
        long siembrasActivas,
        double cosechaKg,
        long trabajadoresActivos,
        long nominasPorPagar,
        BigDecimal totalPagado
) {}
