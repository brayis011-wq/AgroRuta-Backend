package com.agroruta.worker.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LaborTest {

    @Test
    void deberiaCrearActividadConValoresCorrectos() {
        Labor actividad = new Labor(1L, "Riego", "Riego de cultivos");

        assertEquals(1L, actividad.getId());
        assertEquals("Riego", actividad.getNombre());
        assertEquals("Riego de cultivos", actividad.getDescripcion());
    }

    @Test
    void deberiaEstarActivaAlCrearse() {
        Labor actividad = new Labor(1L, "Riego", "Riego de cultivos");

        assertTrue(actividad.isActiva());
    }

    @Test
    void deberiaDesactivarActividad() {
        Labor actividad = new Labor(1L, "Riego", "Riego de cultivos");

        actividad.desactivar();

        assertFalse(actividad.isActiva());
    }

    @Test
    void deberiaActivarActividadDesactivada() {
        Labor actividad = new Labor(1L, "Riego", "Riego de cultivos");
        actividad.desactivar();

        actividad.activar();

        assertTrue(actividad.isActiva());
    }

    @Test
    void deberiaPermitirCambiarNombre() {
        Labor actividad = new Labor(1L, "Riego", "Riego de cultivos");

        actividad.setNombre("Poda");

        assertEquals("Poda", actividad.getNombre());
    }

    @Test
    void deberiaCrearActividadVaciaConNoArgsConstructor() {
        Labor actividad = new Labor();

        assertNull(actividad.getId());
        assertNull(actividad.getNombre());
        assertFalse(actividad.isActiva());
    }
}