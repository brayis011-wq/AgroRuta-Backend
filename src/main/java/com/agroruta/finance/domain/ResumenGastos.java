package com.agroruta.finance.domain;

import java.math.BigDecimal;
import java.util.List;

/** Cuánto suman los gastos generales en un periodo, por categoría y por finca. */
public record ResumenGastos(BigDecimal total, int cantidad, List<Parte> porCategoria, List<Parte> porFinca) {

    /** clave: la categoría o el id de la finca; nombre: el de la finca (null en las categorías) */
    public record Parte(String clave, String nombre, BigDecimal total, int cantidad) {}

    public static ResumenGastos vacio() {
        return new ResumenGastos(BigDecimal.ZERO, 0, List.of(), List.of());
    }
}
