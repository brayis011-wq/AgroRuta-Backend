package com.agroruta.weather.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * ¿Se puede fumigar un día, y a qué horas? Mira el pronóstico por franjas de tres horas:
 * sirve una franja de día sin lluvia ni viento fuerte, y sin lluvia en la franja siguiente
 * (el producto necesita unas horas seco para que no se lave).
 * Sin base de datos ni red, para poder probarlo.
 */
public final class VentanaFumigacion {

    private VentanaFumigacion() {}

    public enum Estado {
        /** Todo lo que queda del día sirve */
        BUENO,
        /** Solo sirve un rato: hay que fumigar entre ventanaInicio y ventanaFin */
        VENTANA,
        /** Ese día no conviene; ventanaInicio/Fin son la siguiente oportunidad (si la hay) */
        NO
    }

    public enum Motivo { LLUVIA, VIENTO }

    /**
     * @param motivo        qué estorba (null si el día es bueno)
     * @param problemaDesde cuándo empieza la lluvia o el viento (null si el día es bueno)
     */
    public record Resultado(Estado estado, Motivo motivo, LocalDateTime problemaDesde,
                            LocalDateTime ventanaInicio, LocalDateTime ventanaFin) {}

    /** Vacío si ya no quedan horas de trabajo ese día o el pronóstico no lo cubre. */
    public static Optional<Resultado> para(List<FranjaClima> franjas, LocalDateTime ahora, LocalDate dia) {
        List<FranjaClima> orden = franjas.stream().sorted(Comparator.comparing(FranjaClima::inicio)).toList();
        List<Integer> delDia = new ArrayList<>();
        for (int i = 0; i < orden.size(); i++) {
            FranjaClima f = orden.get(i);
            if (f.inicio().toLocalDate().equals(dia) && f.deDia() && f.fin().isAfter(ahora)) delDia.add(i);
        }
        if (delDia.isEmpty()) return Optional.empty();

        List<Integer> aptas = delDia.stream().filter(i -> apta(orden, i)).toList();
        if (aptas.size() == delDia.size()) {
            return Optional.of(new Resultado(Estado.BUENO, null, null,
                    desde(orden.get(aptas.get(0)), ahora), orden.get(aptas.get(aptas.size() - 1)).fin()));
        }

        // Lo que estorba: la primera lluvia del día (también la de la noche) o, si no llueve, el viento
        FranjaClima lluvia = orden.stream()
                .filter(f -> f.inicio().toLocalDate().equals(dia) && f.fin().isAfter(ahora) && f.llueve())
                .findFirst().orElse(null);
        FranjaClima viento = delDia.stream().map(orden::get).filter(FranjaClima::ventosa).findFirst().orElse(null);
        Motivo motivo = lluvia != null ? Motivo.LLUVIA : Motivo.VIENTO;
        LocalDateTime problema = lluvia != null ? lluvia.inicio() : viento != null ? viento.inicio() : null;

        if (!aptas.isEmpty()) {
            int[] tramo = tramo(orden, aptas.get(0));
            return Optional.of(new Resultado(Estado.VENTANA, motivo, problema,
                    desde(orden.get(tramo[0]), ahora), orden.get(tramo[1]).fin()));
        }
        // Ese día no: la siguiente oportunidad en lo que alcance a ver el pronóstico
        for (int i = delDia.get(delDia.size() - 1) + 1; i < orden.size(); i++) {
            if (orden.get(i).deDia() && apta(orden, i)) {
                int[] tramo = tramo(orden, i);
                return Optional.of(new Resultado(Estado.NO, motivo, problema,
                        orden.get(tramo[0]).inicio(), orden.get(tramo[1]).fin()));
            }
        }
        return Optional.of(new Resultado(Estado.NO, motivo, problema, null, null));
    }

    private static boolean apta(List<FranjaClima> orden, int i) {
        FranjaClima f = orden.get(i);
        boolean llueveDespues = i + 1 < orden.size() && orden.get(i + 1).llueve();
        return !f.llueve() && !f.ventosa() && !llueveDespues;
    }

    /** Franjas aptas seguidas, de día y en la misma fecha, a partir de {@code i}: [primera, última] */
    private static int[] tramo(List<FranjaClima> orden, int i) {
        int fin = i;
        while (fin + 1 < orden.size() && orden.get(fin + 1).deDia() && apta(orden, fin + 1)
                && orden.get(fin + 1).inicio().toLocalDate().equals(orden.get(i).inicio().toLocalDate())) {
            fin++;
        }
        return new int[]{i, fin};
    }

    private static LocalDateTime desde(FranjaClima f, LocalDateTime ahora) {
        return f.inicio().isBefore(ahora) ? ahora : f.inicio();
    }
}
