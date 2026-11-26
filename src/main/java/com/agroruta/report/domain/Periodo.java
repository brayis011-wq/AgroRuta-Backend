package com.agroruta.report.domain;

import com.agroruta.shared.exception.DatoInvalidoException;
import java.time.LocalDate;

/**
 * Fechas que abarca un reporte (las dos incluidas). Cualquiera puede faltar: sin {@code desde}
 * no hay límite hacia atrás y sin {@code hasta} no lo hay hacia adelante. {@link #TODO} es sin filtro.
 */
public record Periodo(LocalDate desde, LocalDate hasta) {

    public static final Periodo TODO = new Periodo(null, null);

    public Periodo {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new DatoInvalidoException("La fecha inicial del periodo no puede ser posterior a la final.");
        }
    }

    public static Periodo de(LocalDate desde, LocalDate hasta) {
        return desde == null && hasta == null ? TODO : new Periodo(desde, hasta);
    }

    public boolean esTodo() {
        return desde == null && hasta == null;
    }

    /** El día cae dentro del periodo */
    public boolean contiene(LocalDate dia) {
        if (dia == null) return esTodo();
        return (desde == null || !dia.isBefore(desde)) && (hasta == null || !dia.isAfter(hasta));
    }

    /**
     * La temporada estuvo en el campo en algún momento del periodo: se sembró antes de que terminara
     * y no había terminado cuando empezó. Una temporada en curso sigue abierta hasta hoy.
     */
    public boolean seCruzaCon(LocalDate inicio, LocalDate fin) {
        if (esTodo()) return true;
        boolean empezoAntesDelFinal = hasta == null || inicio == null || !inicio.isAfter(hasta);
        boolean seguiaAlInicio = desde == null || fin == null || !fin.isBefore(desde);
        return empezoAntesDelFinal && seguiaAlInicio;
    }
}
