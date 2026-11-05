package com.agroruta.crop.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraResumenSiembraTest {

    private final LocalDate hoy = LocalDate.of(2026, 11, 4); // miércoles

    private Siembra siembra(Long id, int plantas, String cultivo) {
        Siembra s = new Siembra(id, LocalDate.of(2026, 3, 2), plantas,
                new VariedadCultivo(1L, "Colombia", true, 1L, cultivo, true), 9L);
        return s;
    }

    private Cosecha cosecha(String fecha, double kg, CalidadCosecha calidad) {
        return new Cosecha(null, LocalDate.parse(fecha), kg, calidad, null, 1L);
    }

    private Fumigacion fumigacion(String fecha, String producto, Integer carencia, Integer reentrada) {
        Fumigacion f = Fumigacion.fromManualEntry(LocalDate.parse(fecha), producto, 1.0, UnidadMedida.LITROS, 1.0, null, 1L);
        f.fijarRestricciones(new RestriccionesFumigacion(carencia, reentrada));
        return f;
    }

    @Test
    void sumaPorCalidadYCalculaRendimiento() {
        ResumenSiembra r = CalculadoraResumenSiembra.calcular(siembra(1L, 1000, "Uchuva"), 2.0,
                List.of(cosecha("2026-10-14", 140, CalidadCosecha.SEGUNDA),
                        cosecha("2026-10-28", 180.5, CalidadCosecha.PRIMERA),
                        cosecha("2026-10-29", 2, CalidadCosecha.TERCERA)),
                List.of(), List.of(), id -> 0.0, hoy);

        assertEquals(322.5, r.totalKg());
        assertEquals(3, r.cosechas());
        assertEquals(180.5, r.kgPrimera());
        assertEquals(0.323, r.kgPorPlanta());
        assertEquals(161.3, r.kgPorHectarea());
    }

    @Test
    void sinPlantasNiAreaNoInventaRendimiento() {
        ResumenSiembra r = CalculadoraResumenSiembra.calcular(siembra(1L, 0, "Uchuva"), null,
                List.of(cosecha("2026-10-14", 10, CalidadCosecha.PRIMERA)), List.of(), List.of(), id -> 0.0, hoy);

        assertNull(r.kgPorPlanta());
        assertNull(r.kgPorHectarea());
    }

    @Test
    void agrupaPorSemanaDeLunesADomingoSinHuecos() {
        List<ResumenSiembra.SemanaCosecha> semanas = CalculadoraResumenSiembra.semanas(List.of(
                cosecha("2026-10-13", 50, CalidadCosecha.PRIMERA),   // martes, semana del 12
                cosecha("2026-10-18", 20, CalidadCosecha.SEGUNDA),   // domingo, misma semana
                cosecha("2026-11-02", 30, CalidadCosecha.PRIMERA))); // lunes, semana del 2 nov

        assertEquals(4, semanas.size());
        assertEquals(LocalDate.of(2026, 10, 12), semanas.get(0).semana());
        assertEquals(70, semanas.get(0).total());
        assertEquals(0, semanas.get(1).total());
        assertEquals(LocalDate.of(2026, 11, 2), semanas.get(3).semana());
    }

    @Test
    void laCarenciaVigenteEsLaQueTerminaMasTarde() {
        ResumenSiembra.Restriccion c = CalculadoraResumenSiembra.restriccionVigente(List.of(
                fumigacion("2026-10-20", "Viejo", 7, null),        // terminó el 27 oct
                fumigacion("2026-10-30", "Mancozeb", 7, 24),       // libre el 6 nov
                fumigacion("2026-11-02", "Spinosad", 1, 4)), hoy, true); // libre el 3 nov

        assertNotNull(c);
        assertEquals("Mancozeb", c.producto());
        assertEquals(LocalDate.of(2026, 11, 6), c.libreDesde());
        assertEquals(2, c.diasRestantes());
    }

    @Test
    void laReentradaSeCuentaEnDiasCompletos() {
        ResumenSiembra.Restriccion r = CalculadoraResumenSiembra.restriccionVigente(List.of(
                fumigacion("2026-11-04", "Abamectina", null, 72)), hoy, false);

        assertEquals(LocalDate.of(2026, 11, 7), r.libreDesde());
        assertEquals(3, r.diasRestantes());
        assertNull(CalculadoraResumenSiembra.restriccionVigente(List.of(
                fumigacion("2026-11-03", "Spinosad", null, 4)), hoy, false));
    }

    @Test
    void sinDatoDeCarenciaNoHayAlerta() {
        assertNull(CalculadoraResumenSiembra.restriccionVigente(List.of(
                fumigacion("2026-11-03", "Manual", null, null)), hoy, true));
    }

    @Test
    void comparaConLasOtrasTemporadasDelLote() {
        Siembra actual = siembra(3L, 1000, "Uchuva");
        Siembra anterior = siembra(2L, 800, "Uchuva");
        anterior.finalizar(LocalDate.of(2025, 12, 1));
        Siembra frijol = siembra(1L, 0, "Fríjol");
        Map<Long, Double> kg = Map.of(2L, 1640.0, 1L, 410.0);

        ResumenSiembra r = CalculadoraResumenSiembra.calcular(actual, 2.0, List.of(), List.of(),
                List.of(actual, anterior, frijol), kg::get, hoy);

        assertEquals(2, r.otrasTemporadas().size());
        ResumenSiembra.TemporadaComparada t = r.otrasTemporadas().get(0);
        assertEquals(2L, t.siembraId());
        assertEquals(2.05, t.kgPorPlanta());
        assertTrue(t.mismoCultivo());
        assertFalse(t.enCurso());
        assertFalse(r.otrasTemporadas().get(1).mismoCultivo());
        assertNull(r.otrasTemporadas().get(1).kgPorPlanta());
    }
}
