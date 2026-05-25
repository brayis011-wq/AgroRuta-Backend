package com.agroruta.worker.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActividadTest {

    @Test
    void deberiaCrearActividadConValoresCorrectos() {
        Actividad actividad = new Actividad(1L, "Riego", "Riego de cultivos");

        assertEquals(1L, actividad.getId());
        assertEquals("Riego", actividad.getNombre());
        assertEquals("Riego de cultivos", actividad.getDescripcion());
    }

    @Test
    void deberiaEstarActivaAlCrearse() {
        Actividad actividad = new Actividad(1L, "Riego", "Riego de cultivos");

        assertTrue(actividad.isActiva());
    }

    @Test
    void deberiaDesactivarActividad() {
        Actividad actividad = new Actividad(1L, "Riego", "Riego de cultivos");

        actividad.desactivar();

        assertFalse(actividad.isActiva());
    }

    @Test
    void deberiaActivarActividadDesactivada() {
        Actividad actividad = new Actividad(1L, "Riego", "Riego de cultivos");
        actividad.desactivar();

        actividad.activar();

        assertTrue(actividad.isActiva());
    }

    @Test
    void deberiaPermitirCambiarNombre() {
        Actividad actividad = new Actividad(1L, "Riego", "Riego de cultivos");

        actividad.setNombre("Poda");

        assertEquals("Poda", actividad.getNombre());
    }

    @Test
    void deberiaCrearActividadVaciaConNoArgsConstructor() {
        Actividad actividad = new Actividad();

        assertNull(actividad.getId());
        assertNull(actividad.getNombre());
        assertFalse(actividad.isActiva());
    }
}