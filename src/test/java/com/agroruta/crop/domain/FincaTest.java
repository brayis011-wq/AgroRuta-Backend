package com.agroruta.crop.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FincaTest {

    @Test
    void deberiaCrearFincaConConstructorCompleto() {
        Finca finca = new Finca(1L, "Finca El Paraíso", "Cundinamarca", 50.0, 100L);

        assertEquals(1L, finca.getId());
        assertEquals("Finca El Paraíso", finca.getNombre());
        assertEquals("Cundinamarca", finca.getUbicacion());
        assertEquals(50.0, finca.getHectareas());
        assertEquals(100L, finca.getAgricultorId());
        assertNotNull(finca.getFechaRegistro());
    }

    @Test
    void deberiaCrearFincaVaciaConConstructorPorDefecto() {
        Finca finca = new Finca();

        assertNull(finca.getId());
        assertNull(finca.getNombre());
        assertNull(finca.getCentroideLat());
        assertNull(finca.getCentroideLng());
    }

    @Test
    void deberiaActualizarCentroide() {
        Finca finca = new Finca(1L, "Finca", "Ubicacion", 10.0, 1L);

        finca.actualizarCentroide(4.7110, -74.0721);

        assertEquals(4.7110, finca.getCentroideLat());
        assertEquals(-74.0721, finca.getCentroideLng());
    }

    @Test
    void deberiaTenerCentroideCuandoAmbosCamposEstanPresentes() {
        Finca finca = new Finca(1L, "Finca", "Ubicacion", 10.0, 1L);
        finca.actualizarCentroide(4.7110, -74.0721);

        assertTrue(finca.tieneCentroide());
    }

    @Test
    void noDeberiaTenerCentroideCuandoFaltaUnCampo() {
        Finca finca = new Finca(1L, "Finca", "Ubicacion", 10.0, 1L);

        assertFalse(finca.tieneCentroide());
    }

    @Test
    void deberiaModificarCamposConSetters() {
        Finca finca = new Finca();

        finca.setId(2L);
        finca.setNombre("Finca Nueva");
        finca.setHectareas(75.5);

        assertEquals(2L, finca.getId());
        assertEquals("Finca Nueva", finca.getNombre());
        assertEquals(75.5, finca.getHectareas());
    }
}