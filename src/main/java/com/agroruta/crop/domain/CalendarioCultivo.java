package com.agroruta.crop.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Arma los eventos del calendario de una o varias siembras: lo registrado, lo programado,
 * el fin de las carencias y reentradas, y lo estimado con el historial
 * (próxima fumigación, próxima etapa y primera cosecha).
 * Sin acceso a base de datos, para poder probarlo.
 */
public final class CalendarioCultivo {

    /** Intervalo entre fumigaciones cuando solo hay una (es el mismo que usa el aviso de "atrasada") */
    static final int INTERVALO_FUMIGACION_DEFECTO = 7;

    private CalendarioCultivo() {}

    /** Todo lo que se sabe de una siembra */
    public record DatosSiembra(Siembra siembra, Long loteId, String loteNombre,
                               List<EtapaSiembra> etapas, List<ActividadCultivo> actividades,
                               List<Fumigacion> fumigaciones, List<Cosecha> cosechas) {}

    /** Promedios de temporadas terminadas de un cultivo */
    public record Historial(Map<EstadoCultivo, Double> diasPorEtapa, Double diasHastaPrimeraCosecha) {
        static final Historial VACIO = new Historial(Map.of(), null);
    }

    /** Promedios por cultivo (tipoCultivo) usando las temporadas terminadas */
    public static Map<String, Historial> historialPorCultivo(List<DatosSiembra> todas) {
        Map<String, List<DatosSiembra>> porCultivo = new HashMap<>();
        for (DatosSiembra d : todas) {
            if (d.siembra().estaActiva() || d.siembra().getTipoCultivo() == null) continue;
            porCultivo.computeIfAbsent(d.siembra().getTipoCultivo(), k -> new ArrayList<>()).add(d);
        }
        Map<String, Historial> res = new HashMap<>();
        porCultivo.forEach((cultivo, lista) -> {
            Map<EstadoCultivo, List<Long>> duraciones = new EnumMap<>(EstadoCultivo.class);
            List<Long> hastaCosecha = new ArrayList<>();
            for (DatosSiembra d : lista) {
                List<EtapaSiembra> etapas = d.etapas().stream()
                        .filter(e -> e.getFecha() != null)
                        .sorted(Comparator.comparing(EtapaSiembra::getFecha).thenComparing(e -> e.getId() == null ? 0 : e.getId()))
                        .toList();
                for (int i = 0; i + 1 < etapas.size(); i++) {
                    EtapaSiembra a = etapas.get(i), b = etapas.get(i + 1);
                    if (b.getEtapa().ordinal() == a.getEtapa().ordinal() + 1) {
                        duraciones.computeIfAbsent(a.getEtapa(), k -> new ArrayList<>())
                                .add(ChronoUnit.DAYS.between(a.getFecha(), b.getFecha()));
                    }
                }
                d.cosechas().stream().map(Cosecha::getFecha).filter(Objects::nonNull).min(LocalDate::compareTo)
                        .ifPresent(primera -> hastaCosecha.add(ChronoUnit.DAYS.between(d.siembra().getFechaSiembra(), primera)));
            }
            Map<EstadoCultivo, Double> prom = new EnumMap<>(EstadoCultivo.class);
            duraciones.forEach((etapa, dias) -> prom.put(etapa, promedio(dias)));
            res.put(cultivo, new Historial(prom, hastaCosecha.isEmpty() ? null : promedio(hastaCosecha)));
        });
        return res;
    }

    public static List<EventoCalendario> eventos(DatosSiembra d, Historial historial, LocalDate hoy) {
        Siembra s = d.siembra();
        Base b = new Base(s.getId(), d.loteId(), d.loteNombre(), s.nombreCompleto());
        List<EventoCalendario> ev = new ArrayList<>();

        ev.add(b.evento(s.getFechaSiembra(), "SIEMBRA", "HECHO", s.nombreCompleto(), null, null));
        for (EtapaSiembra e : d.etapas()) {
            if (e.getEtapa() == EstadoCultivo.GERMINACION || e.getFecha() == null) continue;
            ev.add(b.evento(e.getFecha(), "ETAPA", "HECHO", e.getEtapa().name(), null, null));
        }
        for (ActividadCultivo a : d.actividades()) {
            if (a.getFecha() == null) continue;
            ev.add(b.evento(a.getFecha(), "ACTIVIDAD", a.getFecha().isAfter(hoy) ? "PENDIENTE" : "HECHO",
                    a.getTipo() == null ? "OTRO" : a.getTipo().name(), a.getDescripcion(), a.getId()));
        }
        for (Fumigacion f : d.fumigaciones()) {
            if (f.getFecha() == null) continue;
            ev.add(b.evento(f.getFecha(), "FUMIGACION", "HECHO", f.getProducto(), null, f.getId()));
            if (f.getDiasCarencia() != null && f.getDiasCarencia() > 0) {
                LocalDate libre = f.getFecha().plusDays(f.getDiasCarencia());
                ev.add(b.evento(libre, "FIN_CARENCIA", libre.isAfter(hoy) ? "PENDIENTE" : "HECHO",
                        f.getProducto(), String.valueOf(f.getDiasCarencia()), f.getId()));
            }
            if (f.getReentradaHoras() != null && f.getReentradaHoras() > 0) {
                LocalDate libre = f.getFecha().plusDays((long) Math.ceil(f.getReentradaHoras() / 24.0));
                ev.add(b.evento(libre, "FIN_REENTRADA", libre.isAfter(hoy) ? "PENDIENTE" : "HECHO",
                        f.getProducto(), String.valueOf(f.getReentradaHoras()), f.getId()));
            }
        }
        for (Cosecha c : d.cosechas()) {
            if (c.getFecha() == null) continue;
            ev.add(b.evento(c.getFecha(), "COSECHA", "HECHO",
                    c.getCantidadKg() == null ? "" : String.valueOf(c.getCantidadKg()),
                    c.getCalidad() == null ? null : c.getCalidad().name(), c.getId()));
        }

        if (s.estaActiva()) {
            proximaFumigacion(d.fumigaciones(), hoy).ifPresent(f ->
                    ev.add(b.evento(f.fecha, "FUMIGACION_SUGERIDA", f.atrasada ? "ATRASADO" : "ESTIMADO",
                            null, String.valueOf(f.intervalo), null)));
            Historial h = historial == null ? Historial.VACIO : historial;
            proximaEtapa(s, d.etapas(), h, hoy, d.loteId(), d.loteNombre()).ifPresent(ev::add);
            if (d.cosechas().isEmpty() && h.diasHastaPrimeraCosecha() != null && s.getFechaSiembra() != null) {
                LocalDate fecha = maximo(s.getFechaSiembra().plusDays(Math.round(h.diasHastaPrimeraCosecha())), hoy);
                ev.add(b.evento(fecha, "COSECHA_ESTIMADA", "ESTIMADO", null,
                        String.valueOf(Math.round(h.diasHastaPrimeraCosecha())), null));
            }
        }
        return ev;
    }

    /** Eventos entre dos fechas (incluidas), ordenados por fecha */
    public static List<EventoCalendario> entre(List<EventoCalendario> eventos, LocalDate desde, LocalDate hasta) {
        Predicate<EventoCalendario> dentro = e -> !e.fecha().isBefore(desde) && !e.fecha().isAfter(hasta);
        return eventos.stream().filter(dentro)
                .sorted(Comparator.comparing(EventoCalendario::fecha).thenComparing(EventoCalendario::tipo))
                .toList();
    }

    // ── estimaciones ──────────────────────────────────────────────────

    record Sugerencia(LocalDate fecha, int intervalo, boolean atrasada) {}

    /** Última fumigación + intervalo habitual (mediana entre fumigaciones, o 7 días si hay una sola) */
    static Optional<Sugerencia> proximaFumigacion(List<Fumigacion> fumigaciones, LocalDate hoy) {
        List<LocalDate> fechas = fumigaciones.stream().map(Fumigacion::getFecha).filter(Objects::nonNull)
                .distinct().sorted().toList();
        if (fechas.isEmpty()) return Optional.empty();
        int intervalo = INTERVALO_FUMIGACION_DEFECTO;
        if (fechas.size() >= 2) {
            List<Long> gaps = new ArrayList<>();
            for (int i = 1; i < fechas.size(); i++) gaps.add(ChronoUnit.DAYS.between(fechas.get(i - 1), fechas.get(i)));
            gaps.sort(Long::compare);
            long mediana = gaps.get(gaps.size() / 2);
            intervalo = (int) Math.max(1, mediana);
        }
        LocalDate fecha = fechas.get(fechas.size() - 1).plusDays(intervalo);
        boolean atrasada = fecha.isBefore(hoy);
        return Optional.of(new Sugerencia(atrasada ? hoy : fecha, intervalo, atrasada));
    }

    static Optional<EventoCalendario> proximaEtapa(Siembra s, List<EtapaSiembra> etapas, Historial h, LocalDate hoy,
                                                            Long loteId, String loteNombre) {
        EstadoCultivo actual = s.getEstadoCultivo();
        if (actual == null || actual == EstadoCultivo.FINALIZADO) return Optional.empty();
        Double dias = h.diasPorEtapa().get(actual);
        if (dias == null) return Optional.empty();
        LocalDate desde = etapas.stream().filter(e -> e.getEtapa() == actual && e.getFecha() != null)
                .map(EtapaSiembra::getFecha).max(LocalDate::compareTo)
                .orElse(actual == EstadoCultivo.GERMINACION ? s.getFechaSiembra() : null);
        if (desde == null) return Optional.empty();
        EstadoCultivo siguiente = EstadoCultivo.values()[actual.ordinal() + 1];
        LocalDate fecha = maximo(desde.plusDays(Math.round(dias)), hoy);
        return Optional.of(new EventoCalendario(fecha, "PROXIMA_ETAPA", "ESTIMADO", siguiente.name(),
                String.valueOf(Math.round(dias)), s.getId(), loteId, loteNombre, s.nombreCompleto(), null));
    }

    private static LocalDate maximo(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? b : a;
    }

    private static double promedio(List<Long> v) {
        return v.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private record Base(Long siembraId, Long loteId, String loteNombre, String cultivo) {
        EventoCalendario evento(LocalDate fecha, String tipo, String estado, String titulo, String detalle, Long refId) {
            return new EventoCalendario(fecha, tipo, estado, titulo, detalle, siembraId, loteId, loteNombre, cultivo, refId);
        }
    }
}
