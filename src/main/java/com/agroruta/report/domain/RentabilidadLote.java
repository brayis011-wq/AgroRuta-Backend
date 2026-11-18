package com.agroruta.report.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuánto dejó un lote sumando sus temporadas, y cómo le fue a cada una frente a la anterior.
 * Hace las mismas cuentas que la pestaña de rentabilidad de la aplicación.
 */
public record RentabilidadLote(
        String loteNombre,
        String fincaNombre,
        /** null si el lote no tiene área */
        Double areaHa,
        /** De la más reciente a la más antigua */
        List<Temporada> temporadas,
        double costo,
        double ingresos,
        /** null si ninguna temporada tiene ventas con precio */
        Double ganancia,
        /** Ganancia ÷ ingresos × 100; null sin ventas */
        Double margen,
        Double gananciaPorHa
) {

    public record Temporada(
            Long siembraId,
            String nombre,
            LocalDate fechaSiembra,
            LocalDate fechaFin,
            boolean enCurso,
            double costo,
            double ingresos,
            double kg,
            /** null si ninguna cosecha tiene precio de venta */
            Double ganancia,
            Double margen,
            /** Ganancia ÷ costo × 100: cuánto dejó cada peso invertido */
            Double retorno,
            Double gananciaPorHa,
            /** Cambio de la ganancia frente a la temporada anterior del mismo lote; null si no se puede comparar */
            Double cambio
    ) {}

    /** Agrupa las temporadas por lote; primero los lotes que más dejaron y al final los que no tienen ventas. */
    public static List<RentabilidadLote> porLote(List<TemporadaFinanzas> filas) {
        Map<String, List<TemporadaFinanzas>> grupos = new LinkedHashMap<>();
        for (TemporadaFinanzas f : filas) {
            if (f.costos() == null) continue;
            CropDetail s = f.siembra();
            // Las siembras antiguas no traen el lote: se agrupan por nombre de finca y de lote
            String clave = s.loteId() != null ? "id" + s.loteId()
                    : "n" + texto(s.fincaNombre()) + "|" + texto(s.loteNombre());
            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(f);
        }

        List<RentabilidadLote> lotes = new ArrayList<>();
        for (List<TemporadaFinanzas> grupo : grupos.values()) {
            lotes.add(lote(grupo));
        }
        lotes.sort(Comparator.comparingDouble(
                (RentabilidadLote l) -> l.ganancia() != null ? l.ganancia() : Double.NEGATIVE_INFINITY).reversed());
        return lotes;
    }

    private static RentabilidadLote lote(List<TemporadaFinanzas> grupo) {
        Double area = grupo.stream().map(f -> f.siembra().loteAreaHa())
                .filter(a -> a != null && a > 0).findFirst().orElse(null);
        List<TemporadaFinanzas> antiguaPrimero = new ArrayList<>(grupo);
        antiguaPrimero.sort(Comparator.comparing((TemporadaFinanzas f) -> f.siembra().fechaSiembra(),
                Comparator.nullsFirst(Comparator.naturalOrder())));

        List<Temporada> temporadas = new ArrayList<>();
        Double previa = null;
        boolean hayPrevia = false;
        for (TemporadaFinanzas f : antiguaPrimero) {
            CostosTemporada c = f.costos();
            Double ganancia = c.ganancia();
            Double cambio = hayPrevia && ganancia != null && previa != null ? ganancia - previa : null;
            temporadas.add(new Temporada(
                    f.siembra().siembraId(), f.siembra().variedad(), f.siembra().fechaSiembra(), f.siembra().fechaFin(),
                    !"FINALIZADO".equals(f.siembra().estado()),
                    c.costoTotal(), c.ingresos(), c.kgTotal(), ganancia,
                    ganancia != null ? porcentaje(ganancia, c.ingresos()) : null,
                    ganancia != null ? porcentaje(ganancia, c.costoTotal()) : null,
                    ganancia != null && area != null ? ganancia / area : null,
                    cambio));
            previa = ganancia;
            hayPrevia = true;
        }

        double costo = temporadas.stream().mapToDouble(Temporada::costo).sum();
        double ingresos = temporadas.stream().mapToDouble(Temporada::ingresos).sum();
        boolean conGanancia = temporadas.stream().anyMatch(t -> t.ganancia() != null);
        Double ganancia = conGanancia ? ingresos - costo : null;
        Collections.reverse(temporadas);
        CropDetail primera = grupo.get(0).siembra();
        return new RentabilidadLote(
                primera.loteNombre() != null ? primera.loteNombre() : "—",
                primera.fincaNombre(), area, List.copyOf(temporadas), costo, ingresos, ganancia,
                ganancia != null ? porcentaje(ganancia, ingresos) : null,
                ganancia != null && area != null ? ganancia / area : null);
    }

    private static Double porcentaje(double parte, double total) {
        return total > 0 ? parte / total * 100 : null;
    }

    private static String texto(String t) {
        return t != null ? t : "";
    }
}
