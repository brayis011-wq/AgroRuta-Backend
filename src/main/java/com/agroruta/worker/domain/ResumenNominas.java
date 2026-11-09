package com.agroruta.worker.domain;

import java.math.BigDecimal;

/** Cuántas nóminas hay en un estado y cuánto suman. Alimenta las tarjetas de la lista de nóminas. */
public record ResumenNominas(EstadoNomina estado, long cantidad, BigDecimal total) {

    public ResumenNominas {
        if (total == null) total = BigDecimal.ZERO;
    }
}
