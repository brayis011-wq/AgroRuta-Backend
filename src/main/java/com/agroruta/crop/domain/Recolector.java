package com.agroruta.crop.domain;

import java.math.BigDecimal;

/**
 * Quién recogió una cosecha y cómo se le paga.
 *
 * @param modalidad KILO (kilos × valor del kilo) o DIA (valor del día)
 * @param valorKilo null = el del cargo del trabajador
 * @param valorDia  null = el del cargo del trabajador
 */
public record Recolector(Long trabajadorId, String modalidad, BigDecimal kilos, BigDecimal valorKilo, BigDecimal valorDia) {

    public boolean porKilo() {
        return "KILO".equalsIgnoreCase(modalidad);
    }
}
