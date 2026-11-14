package com.agroruta.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pesos con dos decimales, igual que las columnas decimal(…,2) de la base. */
public final class Dinero {

    private Dinero() {}

    /** Deja el valor con dos decimales (medio centavo sube). Null se queda null. */
    public static BigDecimal redondear(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
