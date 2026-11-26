package com.agroruta.worker.domain;

import java.math.BigDecimal;

/** Lo pagado en un mes ("2026-11"): cuánto suma y cuántos pagos fueron. */
public record PagosDelMes(String mes, BigDecimal total, long cantidad) {
    public PagosDelMes {
        if (total == null) total = BigDecimal.ZERO;
    }
}
