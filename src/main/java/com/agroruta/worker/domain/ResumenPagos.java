package com.agroruta.worker.domain;

import java.math.BigDecimal;

/** Totales de los pagos (con el mismo filtro de la lista): cuántos, cuánto suman y a cuántos trabajadores. */
public record ResumenPagos(long cantidad, BigDecimal total, long trabajadores) {

    public ResumenPagos {
        if (total == null) total = BigDecimal.ZERO;
    }
}
