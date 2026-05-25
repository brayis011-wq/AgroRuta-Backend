package com.agroruta.worker.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class JornalTest {

    private Cargo cargo;
    private Trabajador trabajador;

    @BeforeEach
    void setUp() {
        cargo = new Cargo(1L, "Operario", "Trabajador de campo", new BigDecimal("50000"), true);
        trabajador = new Trabajador(1L, "Juan", "Pérez", "123456789",
                "3001234567", "Calle 1", LocalDate.now(),
                TipoContrato.JORNAL, cargo);
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
        Actividad actividad = new Actividad(1L, "Riego", "Riego de cultivos");

        jornal.agregarActividad(actividad);

        assertEquals(1, jornal.getActividades().size());
    }

    @Test
    void noDeberiaAgregarActividadDuplicada() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");
        Actividad actividad = new Actividad(1L, "Riego", "Riego de cultivos");

        jornal.agregarActividad(actividad);
        jornal.agregarActividad(actividad);

        assertEquals(1, jornal.getActividades().size());
    }

    @Test
    void deberiaRemoverActividad() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");
        Actividad actividad = new Actividad(1L, "Riego", "Riego de cultivos");
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
        Actividad actividad = new Actividad(1L, "Riego", "Riego de cultivos");

        assertThrows(IllegalStateException.class, () -> jornal.agregarActividad(actividad));
    }

    @Test
    void deberiaLanzarExcepcionAlRemoverActividadEnJornalLiquidado() {
        Jornal jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");
        Actividad actividad = new Actividad(1L, "Riego", "Riego de cultivos");
        jornal.agregarActividad(actividad);
        jornal.marcarComoLiquidado();

        assertThrows(IllegalStateException.class, () -> jornal.removerActividad(actividad));
    }
}