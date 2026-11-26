package com.agroruta.crop.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/** Milímetros que cayeron en el pluviómetro de la finca un día (0 = ese día no llovió). */
public record Lluvia(LocalDate fecha, BigDecimal milimetros) {

    /** Lo que llovió en los últimos días, para decidir si regar o cuándo fumigar */
    public record Resumen(
            BigDecimal ultimos7, BigDecimal ultimos30, LocalDate ultimaLluvia, Long diasSinLluvia, List<Lluvia> dias) {}

    public static Resumen resumir(List<Lluvia> dias, LocalDate hoy) {
        BigDecimal siete = suma(dias, hoy.minusDays(6), hoy);
        BigDecimal treinta = suma(dias, hoy.minusDays(29), hoy);
        LocalDate ultima = dias.stream()
                .filter(l -> l.milimetros() != null
                        && l.milimetros().signum() > 0
                        && !l.fecha().isAfter(hoy))
                .map(Lluvia::fecha)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new Resumen(
                siete,
                treinta,
                ultima,
                ultima == null ? null : ChronoUnit.DAYS.between(ultima, hoy),
                dias.stream().sorted(Comparator.comparing(Lluvia::fecha)).toList());
    }

    private static BigDecimal suma(List<Lluvia> dias, LocalDate desde, LocalDate hasta) {
        return dias.stream()
                .filter(l -> !l.fecha().isBefore(desde) && !l.fecha().isAfter(hasta) && l.milimetros() != null)
                .map(Lluvia::milimetros)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
