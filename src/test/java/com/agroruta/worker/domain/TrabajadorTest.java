package com.agroruta.worker.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TrabajadorTest {

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
    void deberiaCrearTrabajadorConValoresCorrectos() {
        assertEquals(1L, trabajador.getId());
        assertEquals("Juan", trabajador.getNombre());
        assertEquals("Pérez", trabajador.getApellido());
        assertEquals("123456789", trabajador.getCedula());
        assertEquals(TipoContrato.JORNAL, trabajador.getTipoContrato());
        assertEquals(cargo, trabajador.getCargo());
    }

    @Test
    void deberiaEstarActivoAlCrearse() {
        assertEquals(EstadoTrabajador.ACTIVO, trabajador.getEstado());
        assertTrue(trabajador.estaActivo());
    }

    @Test
    void deberiaRetornarNombreCompleto() {
        assertEquals("Juan Pérez", trabajador.getNombreCompleto());
    }

    @Test
    void deberiaDesactivarTrabajador() {
        trabajador.desactivar();

        assertEquals(EstadoTrabajador.INACTIVO, trabajador.getEstado());
        assertFalse(trabajador.estaActivo());
    }

    @Test
    void deberiaSuspenderTrabajador() {
        trabajador.suspender();

        assertEquals(EstadoTrabajador.SUSPENDIDO, trabajador.getEstado());
        assertFalse(trabajador.estaActivo());
    }

    @Test
    void deberiaReactivarTrabajador() {
        trabajador.desactivar();

        trabajador.reactivar();

        assertEquals(EstadoTrabajador.ACTIVO, trabajador.getEstado());
        assertTrue(trabajador.estaActivo());
    }

    @Test
    void deberiaCambiarCargo() {
        Cargo nuevoCargo = new Cargo(2L, "Supervisor", "Supervisor de campo", new BigDecimal("80000"), true);

        trabajador.cambiarCargo(nuevoCargo);

        assertEquals(nuevoCargo, trabajador.getCargo());
        assertEquals("Supervisor", trabajador.getCargo().getNombre());
    }

    @Test
    void deberiaCrearTrabajadorVacioConNoArgsConstructor() {
        Trabajador vacio = new Trabajador();

        assertNull(vacio.getId());
        assertNull(vacio.getNombre());
        assertNull(vacio.getEstado());
    }
}