package com.agroruta.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * El precio de referencia de un cultivo.
 *
 * @param fuente       SIPSA, PROPIO, o null si no hay ningún precio
 * @param elegido      true si el producto o el precio los fijó el agricultor
 * @param alternativas otros productos de SIPSA que pueden corresponder al cultivo
 */
public record PrecioCultivo(
        String cultivo,
        String fuente,
        String producto,
        BigDecimal precioKg,
        BigDecimal minimoKg,
        BigDecimal maximoKg,
        LocalDate semana,
        Integer mercados,
        boolean elegido,
        List<PrecioReferencia> alternativas) {

    public static PrecioCultivo sinPrecio(String cultivo, List<PrecioReferencia> alternativas) {
        return new PrecioCultivo(cultivo, null, null, null, null, null, null, null, false, alternativas);
    }

    public static PrecioCultivo deSipsa(
            String cultivo, PrecioReferencia p, boolean elegido, List<PrecioReferencia> alternativas) {
        return new PrecioCultivo(
                cultivo,
                "SIPSA",
                p.producto(),
                p.precioKg(),
                p.minimoKg(),
                p.maximoKg(),
                p.semana(),
                p.mercados(),
                elegido,
                alternativas);
    }

    public static PrecioCultivo propio(String cultivo, BigDecimal precioKg, List<PrecioReferencia> alternativas) {
        return new PrecioCultivo(cultivo, "PROPIO", null, precioKg, null, null, null, null, true, alternativas);
    }
}
