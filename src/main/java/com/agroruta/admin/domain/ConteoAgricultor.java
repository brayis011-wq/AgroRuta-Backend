package com.agroruta.admin.domain;

import java.math.BigDecimal;

/** Totales de un agricultor. */
public record ConteoAgricultor(
        long fincas,
        double hectareas,
        long trabajadoresActivos,
        long nominas,
        BigDecimal totalPagado
) {
    public static final ConteoAgricultor VACIO =
            new ConteoAgricultor(0, 0.0, 0, 0, BigDecimal.ZERO);
}
