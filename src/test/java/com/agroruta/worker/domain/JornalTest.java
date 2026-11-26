package com.agroruta.worker.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.agroruta.shared.exception.EstadoInvalidoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JornalTest {

    private Cargo cargo;
    private Trabajador trabajador;

    @BeforeEach
    void setUp() {
        cargo = new Cargo(1L, "Operario", "Trabajador de campo", new BigDecimal("50000"), true);
        trabajador = new Trabajador(
                1L, "Juan", "Pérez", "123456789", "3001234567", "Calle 1", LocalDate.now(), TipoContrato.JORNAL, cargo);
    }

    @Test
    void deberiaCrearJornalConValoresCorrectos() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");

        assertEquals(1L, jornal.getId());
        assertEquals(trabajador, jornal.getTrabajador());
        assertEquals(10L, jornal.getCultivoId());
        assertEquals("Maíz", jornal.getNombreCultivo());
        assertFalse(jornal.isLiquidado());
    }

    @Test
    void deberiaTomarValorJornalDelCargo() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");

        assertEquals(new BigDecimal("50000"), jornal.getValorJornal());
    }

    @Test
    void deberiaCrearJornalVacioConListaDeActividades() {
        Jornal jornal = new Jornal();

        assertNotNull(jornal.getActividades());
        assertTrue(jornal.getActividades().isEmpty());
        assertFalse(jornal.isLiquidado());
    }

    @Test
    void deberiaAgregarActividad() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");
        Labor actividad = new Labor(1L, "Riego", "Riego de cultivos");

        jornal.agregarActividad(actividad);

        assertEquals(1, jornal.getActividades().size());
    }

    @Test
    void noDeberiaAgregarActividadDuplicada() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");
        Labor actividad = new Labor(1L, "Riego", "Riego de cultivos");

        jornal.agregarActividad(actividad);
        jornal.agregarActividad(actividad);

        assertEquals(1, jornal.getActividades().size());
    }

    @Test
    void deberiaRemoverActividad() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");
        Labor actividad = new Labor(1L, "Riego", "Riego de cultivos");
        jornal.agregarActividad(actividad);

        jornal.removerActividad(actividad);

        assertTrue(jornal.getActividades().isEmpty());
    }

    @Test
    void deberiaMarcarComoLiquidado() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");

        jornal.marcarComoLiquidado();

        assertTrue(jornal.isLiquidado());
    }

    @Test
    void deberiaLanzarExcepcionAlAgregarActividadEnJornalLiquidado() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");
        jornal.marcarComoLiquidado();
        Labor actividad = new Labor(1L, "Riego", "Riego de cultivos");

        assertThrows(EstadoInvalidoException.class, () -> jornal.agregarActividad(actividad));
    }

    @Test
    void deberiaLanzarExcepcionAlRemoverActividadEnJornalLiquidado() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");
        Labor actividad = new Labor(1L, "Riego", "Riego de cultivos");
        jornal.agregarActividad(actividad);
        jornal.marcarComoLiquidado();

        assertThrows(EstadoInvalidoException.class, () -> jornal.removerActividad(actividad));
    }

    @Test
    void enLaCosechaSePuedePagarPorKiloAunqueElCargoSeaPorDia() {
        Jornal jornal = new Jornal(null, LocalDate.now(), trabajador, 10L, "Café", null);
        jornal.pagarPorKilo(new BigDecimal("70"), new BigDecimal("900"));
        assertEquals(0, new BigDecimal("63000").compareTo(jornal.getValorJornal()));
        assertEquals(0, new BigDecimal("70").compareTo(jornal.getKilos()));

        // Sin valor del kilo y con un cargo que no lo tiene, se pide
        Jornal otro = new Jornal(null, LocalDate.now(), trabajador, 10L, "Café", null);
        var e = assertThrows(
                com.agroruta.shared.exception.DatoInvalidoException.class,
                () -> otro.pagarPorKilo(new BigDecimal("10"), null));
        assertTrue(e.getMessage().contains("kilo"));
    }

    @Test
    void enLaCosechaPorDiaTomaElValorIndicadoOElDelCargo() {
        Jornal jornal = new Jornal(null, LocalDate.now(), trabajador, 10L, "Café", null);
        jornal.pagarPorKilo(new BigDecimal("10"), new BigDecimal("900"));
        jornal.pagarPorDia(null);
        assertEquals(0, new BigDecimal("50000").compareTo(jornal.getValorJornal()));
        assertNull(jornal.getKilos());
        jornal.pagarPorDia(new BigDecimal("55000"));
        assertEquals(0, new BigDecimal("55000").compareTo(jornal.getValorJornal()));
    }
}
