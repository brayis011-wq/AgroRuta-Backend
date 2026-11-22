package com.agroruta.worker.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cuánto recoge un trabajador y cuánto sale el kilo, separando los días pagados por día (con los kilos que
 * se anotaron) de los pagados por kilo. Sirve para ver si conviene pagarle de una forma o de la otra.
 */
public record RendimientoTrabajador(Long trabajadorId, String nombre, String cargo, String modalidadCargo,
                                    int diasPorDia, BigDecimal kilosPorDia, BigDecimal pagadoPorDia,
                                    int diasPorKilo, BigDecimal kilosPorKilo, BigDecimal pagadoPorKilo) {

    /** Kilos que recoge en un día, contando todos los días con kilos anotados */
    @com.fasterxml.jackson.annotation.JsonProperty("kilosAlDia")
    public BigDecimal kilosAlDia() {
        int dias = diasPorDia + diasPorKilo;
        return dias == 0 ? null : kilosPorDia.add(kilosPorKilo).divide(BigDecimal.valueOf(dias), 1, RoundingMode.HALF_UP);
    }

    /** Lo que costó cada kilo en los días pagados por día (null si no hay) */
    @com.fasterxml.jackson.annotation.JsonProperty("costoKiloPorDia")
    public BigDecimal costoKiloPorDia() {
        return kilosPorDia.signum() > 0 ? pagadoPorDia.divide(kilosPorDia, 0, RoundingMode.HALF_UP) : null;
    }

    /** Lo que costó cada kilo en los días pagados por kilo (null si no hay) */
    @com.fasterxml.jackson.annotation.JsonProperty("costoKiloPorKilo")
    public BigDecimal costoKiloPorKilo() {
        return kilosPorKilo.signum() > 0 ? pagadoPorKilo.divide(kilosPorKilo, 0, RoundingMode.HALF_UP) : null;
    }
}
