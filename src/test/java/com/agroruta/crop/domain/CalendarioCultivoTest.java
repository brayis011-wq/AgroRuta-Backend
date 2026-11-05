package com.agroruta.crop.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalendarioCultivoTest {

    private final LocalDate hoy = LocalDate.of(2026, 11, 4);

    private Siembra siembra(Long id, String fecha) {
        return new Siembra(id, LocalDate.parse(fecha), 100,
                new VariedadCultivo(1L, "Colombia", true, 1L, "Uchuva", true), 9L);
    }

    private EtapaSiembra etapa(Long siembra, EstadoCultivo e, String fecha) {
        return new EtapaSiembra(null, siembra, e, LocalDate.parse(fecha));
    }

    private Fumigacion fumigacion(String fecha, Integer carencia, Integer reentrada) {
        Fumigacion f = Fumigacion.fromManualEntry(LocalDate.parse(fecha), "Mancozeb", 1.0, UnidadMedida.KG, 1.0, null, 1L);
        f.fijarRestricciones(new RestriccionesFumigacion(carencia, reentrada));
        return f;
    }

    private CalendarioCultivo.DatosSiembra datos(Siembra s, List<EtapaSiembra> etapas, List<ActividadCultivo> act,
                                                 List<Fumigacion> fum, List<Cosecha> cos) {
        return new CalendarioCultivo.DatosSiembra(s, 9L, "Lote 1", etapas, act, fum, cos);
    }

    private List<String> tipos(List<EventoCalendario> ev) {
        return ev.stream().map(e -> e.tipo() + ":" + e.estado()).toList();
    }

    @Test
    void incluyeLoRegistradoLoProgramadoYElFinDeLasRestricciones() {
        Siembra s = siembra(1L, "2026-09-01");
        List<EventoCalendario> ev = CalendarioCultivo.eventos(datos(s, List.of(),
                List.of(new ActividadCultivo(5L, TipoActividad.PODA, "Poda", LocalDate.of(2026, 11, 10), 1L)),
                List.of(fumigacion("2026-11-02", 7, 24)),
                List.of()), null, hoy);

        List<String> t = tipos(ev);
        assertTrue(t.contains("SIEMBRA:HECHO"));
        assertTrue(t.contains("ACTIVIDAD:PENDIENTE"));
        assertTrue(t.contains("FUMIGACION:HECHO"));
        assertTrue(t.contains("FIN_CARENCIA:PENDIENTE"));
        assertTrue(t.contains("FIN_REENTRADA:HECHO"));   // 2 nov + 1 día = 3 nov, ya pasó
        EventoCalendario carencia = ev.stream().filter(e -> e.tipo().equals("FIN_CARENCIA")).findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 11, 9), carencia.fecha());
        assertEquals("Lote 1", carencia.loteNombre());
    }

    @Test
    void sugiereLaProximaFumigacionConElIntervaloHabitual() {
        var s = CalendarioCultivo.proximaFumigacion(List.of(
                fumigacion("2026-10-01", null, null), fumigacion("2026-10-11", null, null),
                fumigacion("2026-10-21", null, null), fumigacion("2026-10-31", null, null)), hoy).orElseThrow();
        assertEquals(10, s.intervalo());
        assertEquals(LocalDate.of(2026, 11, 10), s.fecha());
        assertFalse(s.atrasada());
    }

    @Test
    void unaFumigacionVencidaQuedaHoyComoAtrasada() {
        var s = CalendarioCultivo.proximaFumigacion(List.of(fumigacion("2026-10-20", null, null)), hoy).orElseThrow();
        assertEquals(7, s.intervalo());
        assertEquals(hoy, s.fecha());
        assertTrue(s.atrasada());
    }

    @Test
    void estimaEtapaYPrimeraCosechaConTemporadasAnteriores() {
        Siembra vieja = siembra(2L, "2025-01-01");
        vieja.finalizar(LocalDate.of(2025, 9, 1));
        var hist = CalendarioCultivo.historialPorCultivo(List.of(datos(vieja,
                List.of(etapa(2L, EstadoCultivo.GERMINACION, "2025-01-01"), etapa(2L, EstadoCultivo.CRECIMIENTO, "2025-02-10"),
                        etapa(2L, EstadoCultivo.PRODUCCION, "2025-05-01")),
                List.of(), List.of(), List.of(new Cosecha(null, LocalDate.of(2025, 5, 31), 10.0, CalidadCosecha.PRIMERA, null, 2L))))).get("Uchuva");

        assertEquals(40.0, hist.diasPorEtapa().get(EstadoCultivo.GERMINACION));
        assertEquals(150.0, hist.diasHastaPrimeraCosecha());

        Siembra actual = siembra(1L, "2026-10-01");
        List<EventoCalendario> ev = CalendarioCultivo.eventos(datos(actual,
                List.of(etapa(1L, EstadoCultivo.GERMINACION, "2026-10-01")), List.of(), List.of(), List.of()), hist, hoy);

        EventoCalendario etapa = ev.stream().filter(e -> e.tipo().equals("PROXIMA_ETAPA")).findFirst().orElseThrow();
        assertEquals("CRECIMIENTO", etapa.titulo());
        assertEquals(LocalDate.of(2026, 11, 10), etapa.fecha());
        EventoCalendario cosecha = ev.stream().filter(e -> e.tipo().equals("COSECHA_ESTIMADA")).findFirst().orElseThrow();
        assertEquals(LocalDate.of(2027, 2, 28), cosecha.fecha());
    }

    @Test
    void sinHistorialNoEstimaYUnaSiembraTerminadaNoTienePendientes() {
        Siembra terminada = siembra(1L, "2026-01-01");
        terminada.finalizar(LocalDate.of(2026, 6, 1));
        List<EventoCalendario> ev = CalendarioCultivo.eventos(datos(terminada, List.of(), List.of(),
                List.of(fumigacion("2026-05-01", null, null)), List.of()), null, hoy);

        assertTrue(ev.stream().noneMatch(e -> e.estado().equals("ESTIMADO") || e.estado().equals("ATRASADO")));
    }

    @Test
    void filtraPorRangoYOrdenaPorFecha() {
        Siembra s = siembra(1L, "2026-09-01");
        List<EventoCalendario> ev = CalendarioCultivo.eventos(datos(s, List.of(), List.of(),
                List.of(fumigacion("2026-10-01", null, null), fumigacion("2026-11-01", null, null)), List.of()), null, hoy);

        List<EventoCalendario> nov = CalendarioCultivo.entre(ev, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertTrue(nov.stream().allMatch(e -> e.fecha().getMonthValue() == 11));
        assertEquals(LocalDate.of(2026, 11, 1), nov.get(0).fecha());
    }
}
