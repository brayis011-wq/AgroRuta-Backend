package com.agroruta.crop.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/** Cálculos del resumen de una siembra (sin acceso a base de datos, para poder probarlos). */
public final class CalculadoraResumenSiembra {

    /** Semanas que se muestran en la gráfica como máximo */
    static final int MAX_SEMANAS = 26;

    private CalculadoraResumenSiembra() {}

    public static ResumenSiembra calcular(Siembra siembra, Double areaLote,
                                          List<Cosecha> cosechas, List<Fumigacion> fumigaciones,
                                          List<Siembra> temporadasDelLote, Function<Long, Double> kgDeSiembra,
                                          LocalDate hoy) {
        double primera = suma(cosechas, CalidadCosecha.PRIMERA);
        double segunda = suma(cosechas, CalidadCosecha.SEGUNDA);
        double tercera = suma(cosechas, CalidadCosecha.TERCERA);
        double total = primera + segunda + tercera;

        Integer plantas = siembra.getCantidadPlantas();
        Double porPlanta = plantas != null && plantas > 0 ? redondear(total / plantas, 3) : null;
        Double porHa = areaLote != null && areaLote > 0 ? redondear(total / areaLote, 1) : null;

        List<ResumenSiembra.TemporadaComparada> otras = new ArrayList<>();
        for (Siembra s : temporadasDelLote) {
            if (Objects.equals(s.getId(), siembra.getId())) continue;
            double kg = Optional.ofNullable(kgDeSiembra.apply(s.getId())).orElse(0.0);
            Integer p = s.getCantidadPlantas();
            otras.add(new ResumenSiembra.TemporadaComparada(
                    s.getId(), s.nombreCompleto(), s.getFechaSiembra(), s.getFechaFin(), s.estaActiva(),
                    redondear(kg, 1), p != null && p > 0 ? redondear(kg / p, 3) : null,
                    Objects.equals(s.getTipoCultivo(), siembra.getTipoCultivo())));
        }

        return new ResumenSiembra(
                siembra.getId(), redondear(total, 1), cosechas.size(),
                redondear(primera, 1), redondear(segunda, 1), redondear(tercera, 1),
                porPlanta, porHa, areaLote,
                semanas(cosechas),
                restriccionVigente(fumigaciones, hoy, true),
                restriccionVigente(fumigaciones, hoy, false),
                otras);
    }

    /** Kilos por semana (lunes a domingo), sin huecos entre la primera y la última cosecha */
    static List<ResumenSiembra.SemanaCosecha> semanas(List<Cosecha> cosechas) {
        TreeMap<LocalDate, double[]> porSemana = new TreeMap<>();
        for (Cosecha c : cosechas) {
            if (c.getFecha() == null || c.getCantidadKg() == null) continue;
            double[] kg = porSemana.computeIfAbsent(lunes(c.getFecha()), k -> new double[3]);
            int i = c.getCalidad() == null ? 2 : c.getCalidad().ordinal();
            kg[Math.min(i, 2)] += c.getCantidadKg();
        }
        if (porSemana.isEmpty()) return List.of();

        LocalDate ultima = porSemana.lastKey();
        LocalDate desde = porSemana.firstKey();
        LocalDate limite = ultima.minusWeeks(MAX_SEMANAS - 1L);
        if (desde.isBefore(limite)) desde = limite;

        List<ResumenSiembra.SemanaCosecha> lista = new ArrayList<>();
        for (LocalDate s = desde; !s.isAfter(ultima); s = s.plusWeeks(1)) {
            double[] kg = porSemana.getOrDefault(s, new double[3]);
            lista.add(new ResumenSiembra.SemanaCosecha(s, redondear(kg[0], 1), redondear(kg[1], 1), redondear(kg[2], 1)));
        }
        return lista;
    }

    /**
     * La restricción que termina más tarde y todavía no ha terminado.
     * Carencia: se puede cosechar desde fecha + días. Reentrada: se cuenta en días completos
     * desde el día de la aplicación (24 h = al día siguiente).
     */
    static ResumenSiembra.Restriccion restriccionVigente(List<Fumigacion> fumigaciones, LocalDate hoy, boolean carencia) {
        return fumigaciones.stream()
                .filter(f -> f.getFecha() != null)
                .map(f -> {
                    Integer valor = carencia ? f.getDiasCarencia() : f.getReentradaHoras();
                    if (valor == null || valor <= 0) return null;
                    long dias = carencia ? valor : (long) Math.ceil(valor / 24.0);
                    LocalDate libre = f.getFecha().plusDays(dias);
                    return new ResumenSiembra.Restriccion(f.getProducto(), f.getFecha(), libre,
                            ChronoUnit.DAYS.between(hoy, libre), valor);
                })
                .filter(Objects::nonNull)
                .filter(r -> r.libreDesde().isAfter(hoy))
                .max(Comparator.comparing(ResumenSiembra.Restriccion::libreDesde))
                .orElse(null);
    }

    static LocalDate lunes(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static double suma(List<Cosecha> cosechas, CalidadCosecha calidad) {
        return cosechas.stream()
                .filter(c -> c.getCalidad() == calidad && c.getCantidadKg() != null)
                .mapToDouble(Cosecha::getCantidadKg).sum();
    }

    private static double redondear(double v, int decimales) {
        double f = Math.pow(10, decimales);
        return Math.round(v * f) / f;
    }
}
