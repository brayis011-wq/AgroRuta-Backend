package com.agroruta.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Precio mayorista de un producto en una semana: el promedio de lo que reportaron los mercados
 * del país a SIPSA (DANE), en pesos por kilo.
 */
public record PrecioReferencia(
        String producto,
        LocalDate semana,
        BigDecimal precioKg,
        BigDecimal minimoKg,
        BigDecimal maximoKg,
        int mercados) {}
