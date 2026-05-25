package com.agroruta.worker.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CargoTest {

    @Test
    void deberiaCrearCargoConValoresCorrectos() {
        Cargo cargo = new Cargo(1L, "Operario", "Trabajador de campo", new BigDecimal("50000"), true);

        assertEquals(1L, cargo.getId());
        assertEquals("Operario", cargo.getNombre());
        assertEquals("Trabajador de campo", cargo.getDescripcion());
        assertEquals(new BigDecimal("50000"), cargo.getValorJornal());
        assertTrue(cargo.isActivo());
    }

    @Test
    void deberiaDesactivarCargo() {
        Cargo cargo = new Cargo(1L, "Operario", "Trabajador de campo", new BigDecimal("50000"), true);

        cargo.desactivar();

        assertFalse(cargo.isActivo());
    }

    @Test
    void deberiaActivarCargoDesactivado() {
        Cargo cargo = new Cargo(1L, "Operario", "Trabajador de campo", new BigDecimal("50000"), false);

        cargo.activar();

        assertTrue(cargo.isActivo());
    }

    @Test
    void deberiaActualizarDatosCargo() {
        Cargo cargo = new Cargo(1L, "Operario", "Trabajador de campo", new BigDecimal("50000"), true);

        cargo.actualizar("Supervisor", "Supervisor de campo", new BigDecimal("80000"));

        assertEquals("Supervisor", cargo.getNombre());
        assertEquals("Supervisor de campo", cargo.getDescripcion());
        assertEquals(new BigDecimal("80000"), cargo.getValorJornal());
    }

    @Test
    void deberiaCrearCargoVacioConNoArgsConstructor() {
        Cargo cargo = new Cargo();

        assertNull(cargo.getId());
        assertNull(cargo.getNombre());
        assertFalse(cargo.isActivo());
    }
}