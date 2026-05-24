package com.agroruta.crop.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ActividadCultivoTest {

    @Test
    void deberiaCrearActividadConConstructorCompleto() {
        LocalDate fecha = LocalDate.of(2026, 1, 15);

        ActividadCultivo actividad = new ActividadCultivo(
                1L, TipoActividad.RIEGO, "Riego por goteo", fecha, 10L);

        assertEquals(1L, actividad.getId());
        assertEquals(TipoActividad.RIEGO, actividad.getTipo());
        assertEquals("Riego por goteo", actividad.getDescripcion());
        assertEquals(fecha, actividad.getFecha());
        assertEquals(10L, actividad.getSiembraId());
    }

    @Test
    void deberiaCrearActividadVaciaConConstructorPorDefecto() {
        ActividadCultivo actividad = new ActividadCultivo();

        assertNull(actividad.getId());
        assertNull(actividad.getTipo());
        assertNull(actividad.getDescripcion());
        assertNull(actividad.getFecha());
        assertNull(actividad.getSiembraId());
    }

    @Test
    void deberiaModificarCamposConSetters() {
        ActividadCultivo actividad = new ActividadCultivo();
        LocalDate fecha = LocalDate.of(2026, 3, 10);

        actividad.setId(2L);
        actividad.setTipo(TipoActividad.PODA);
        actividad.setDescripcion("Poda de formación");
        actividad.setFecha(fecha);
        actividad.setSiembraId(5L);

        assertEquals(2L, actividad.getId());
        assertEquals(TipoActividad.PODA, actividad.getTipo());
        assertEquals("Poda de formación", actividad.getDescripcion());
        assertEquals(fecha, actividad.getFecha());
        assertEquals(5L, actividad.getSiembraId());
    }

    @Test
    void deberiaSoportarTodosLosTiposDeActividad() {
        for (TipoActividad tipo : TipoActividad.values()) {
            ActividadCultivo actividad = new ActividadCultivo(
                    1L, tipo, "descripción", LocalDate.now(), 1L);

            assertEquals(tipo, actividad.getTipo());
        }
    }
}