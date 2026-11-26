package com.agroruta.crop.application;

import static org.junit.jupiter.api.Assertions.*;

import com.agroruta.crop.application.ports.out.ManoObraPort.JornalDeSiembra;
import com.agroruta.crop.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraCostosSiembraTest {

    private JornalDeSiembra jornal(String fecha, long trabajador, String nombre, int valor) {
        return new JornalDeSiembra(LocalDate.parse(fecha), trabajador, nombre, BigDecimal.valueOf(valor));
    }

    private static BigDecimal pesos(Double v) {
        return v == null ? null : BigDecimal.valueOf(v);
    }

    private Fumigacion fumigacion(String fecha, Double costo) {
        Fumigacion f =
                Fumigacion.fromManualEntry(LocalDate.parse(fecha), "Mancozeb", 1.0, UnidadMedida.KG, 1.0, null, 1L);
        f.fijarRestricciones(new RestriccionesFumigacion(null, null, pesos(costo)));
        return f;
    }

    private ActividadCultivo actividad(String fecha, Double costo) {
        ActividadCultivo a = new ActividadCultivo(null, TipoActividad.TUTOREO, "Tutores", LocalDate.parse(fecha), 1L);
        a.setCosto(pesos(costo));
        return a;
    }

    private Cosecha cosecha(String fecha, double kg, Double precio) {
        Cosecha c = new Cosecha(null, LocalDate.parse(fecha), kg, CalidadCosecha.PRIMERA, null, 1L);
        c.setPrecioKg(pesos(precio));
        return c;
    }

    @Test
    void sumaManoObraInsumosOtrosEIngresos() {
        CostosSiembra c = CalculadoraCostosSiembra.calcular(
                1L,
                List.of(
                        jornal("2026-09-02", 1, "Juan Pérez", 60000),
                        jornal("2026-09-03", 1, "Juan Pérez", 60000),
                        jornal("2026-10-01", 2, "Ana Gómez", 70000)),
                List.of(fumigacion("2026-09-10", 45000.0), fumigacion("2026-10-10", null)),
                List.of(actividad("2026-09-05", 120000.0), actividad("2026-10-02", null)),
                List.of(cosecha("2026-10-20", 100, 4500.0), cosecha("2026-10-27", 50, null)));

        assertEquals(190000, c.manoObra());
        assertEquals(3, c.jornales());
        assertEquals(45000, c.insumos());
        assertEquals(120000, c.otros());
        assertEquals(355000, c.costoTotal());
        assertEquals(450000, c.ingresos());
        assertEquals(150, c.kgTotal());
        assertEquals(100, c.kgConPrecio());
        assertEquals(4500, c.precioPromedioKg());
        assertEquals(95000, c.ganancia());
        assertEquals(2367, c.costoPorKg());
        assertEquals(1, c.fumigacionesSinCosto());
        assertEquals(1, c.cosechasSinPrecio());
    }

    @Test
    void agrupaPorTrabajadorYPorMes() {
        CostosSiembra c = CalculadoraCostosSiembra.calcular(
                1L,
                List.of(
                        jornal("2026-09-02", 1, "Juan Pérez", 60000),
                        jornal("2026-09-03", 1, "Juan Pérez", 60000),
                        jornal("2026-10-01", 2, "Ana Gómez", 70000)),
                List.of(fumigacion("2026-10-10", 45000.0)),
                List.of(),
                List.of(cosecha("2026-10-20", 10, 5000.0)));

        assertEquals("Juan Pérez", c.trabajadores().get(0).nombre());
        assertEquals(2, c.trabajadores().get(0).jornales());
        assertEquals(120000, c.trabajadores().get(0).valor());
        assertEquals(2, c.meses().size());
        assertEquals("2026-09", c.meses().get(0).mes());
        assertEquals(120000, c.meses().get(0).manoObra());
        assertEquals(45000, c.meses().get(1).insumos());
        assertEquals(50000, c.meses().get(1).ingresos());
    }

    @Test
    void sinPreciosNoInventaGanancia() {
        CostosSiembra c = CalculadoraCostosSiembra.calcular(
                1L, List.of(), List.of(), List.of(), List.of(cosecha("2026-10-20", 10, null)));

        assertNull(c.ganancia());
        assertNull(c.precioPromedioKg());
        assertEquals(0, c.costoPorKg());
    }

    @Test
    void losCentavosSeSumanExactos() {
        // 0,70 + 1,40 + 1,40 = 3,50 → 4 pesos. Sumado en double daba 3,4999999999999996 → 3
        CostosSiembra c = CalculadoraCostosSiembra.calcular(
                1L,
                List.of(),
                List.of(),
                List.of(actividad("2026-10-01", 0.7), actividad("2026-10-02", 1.4), actividad("2026-10-03", 1.4)),
                List.of());

        assertEquals(4, c.otros());
        assertEquals(4, c.costoTotal());
        assertEquals(4, c.meses().get(0).otros());
    }

    @Test
    void sinCosechasNoHayCostoPorKilo() {
        CostosSiembra c = CalculadoraCostosSiembra.calcular(1L, List.of(), List.of(), List.of(), List.of());
        assertNull(c.costoPorKg());
    }

    @Test
    void conVentasLosIngresosSonLoVendidoYNoElPrecioDeLaCosecha() {
        var ventas = List.of(
                new com.agroruta.crop.application.ports.out.VentasSiembraPort.VentaDeSiembra(
                        LocalDate.parse("2026-10-22"), new BigDecimal("80"), new BigDecimal("960000")),
                new com.agroruta.crop.application.ports.out.VentasSiembraPort.VentaDeSiembra(
                        LocalDate.parse("2026-11-02"), null, new BigDecimal("40000")));
        CostosSiembra c = CalculadoraCostosSiembra.calcular(
                1L,
                List.of(jornal("2026-10-20", 1, "Leo Paz", 63000)),
                List.of(),
                List.of(),
                List.of(cosecha("2026-10-20", 100, 4500.0), cosecha("2026-10-27", 50, null)),
                ventas);

        assertEquals("VENTAS", c.fuenteIngresos());
        assertEquals(1_000_000, c.ingresos());
        assertEquals(80, c.kgVendidos());
        assertEquals(150, c.kgTotal());
        assertEquals(12500, c.precioPromedioKg());
        assertEquals(937_000, c.ganancia());
        assertEquals(0, c.cosechasSinPrecio());
        assertEquals(2, c.meses().size());
    }

    @Test
    void sinVentasNiPreciosNoHayFuenteDeIngresos() {
        CostosSiembra c = CalculadoraCostosSiembra.calcular(
                1L, List.of(), List.of(), List.of(), List.of(cosecha("2026-10-20", 100, null)), List.of());
        assertNull(c.fuenteIngresos());
        assertNull(c.ganancia());
    }
}
