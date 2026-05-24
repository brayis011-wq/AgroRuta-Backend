package com.agroruta.crop.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class CosechaTest {

    @Test
    void deberiaCrearCosechaConConstructorCompleto() {
        LocalDate fecha = LocalDate.of(2026, 3, 15);

        Cosecha cosecha = new Cosecha(1L, fecha, 250.5,
                CalidadCosecha.PRIMERA, "Buena cosecha", 10L);

        assertEquals(1L, cosecha.getId());
        assertEquals(fecha, cosecha.getFecha());
        assertEquals(250.5, cosecha.getCantidadKg());
        assertEquals(CalidadCosecha.PRIMERA, cosecha.getCalidad());
        assertEquals("Buena cosecha", cosecha.getObservaciones());
        assertEquals(10L, cosecha.getSiembraId());
    }

    @Test
    void deberiaCrearCosechaVaciaConConstructorPorDefecto() {
        Cosecha cosecha = new Cosecha();

        assertNull(cosecha.getId());
        assertNull(cosecha.getFecha());
        assertNull(cosecha.getCantidadKg());
        assertNull(cosecha.getCalidad());
        assertNull(cosecha.getObservaciones());
        assertNull(cosecha.getSiembraId());
    }

    @Test
    void deberiaModificarCamposConSetters() {
        Cosecha cosecha = new Cosecha();
        LocalDate fecha = LocalDate.of(2026, 5, 20);

        cosecha.setId(2L);
        cosecha.setFecha(fecha);
        cosecha.setCantidadKg(100.0);
        cosecha.setCalidad(CalidadCosecha.SEGUNDA);
        cosecha.setObservaciones("Cosecha regular");
        cosecha.setSiembraId(3L);

        assertEquals(2L, cosecha.getId());
        assertEquals(fecha, cosecha.getFecha());
        assertEquals(100.0, cosecha.getCantidadKg());
        assertEquals(CalidadCosecha.SEGUNDA, cosecha.getCalidad());
        assertEquals("Cosecha regular", cosecha.getObservaciones());
        assertEquals(3L, cosecha.getSiembraId());
    }

    @Test
    void deberiaSoportarTodasLasCalidades() {
        for (CalidadCosecha calidad : CalidadCosecha.values()) {
            Cosecha cosecha = new Cosecha(1L, LocalDate.now(), 100.0,
                    calidad, "obs", 1L);

            assertEquals(calidad, cosecha.getCalidad());
        }
    }

    @Test
    void deberiaPermitirObservacionesNulas() {
        Cosecha cosecha = new Cosecha(1L, LocalDate.now(), 50.0,
                CalidadCosecha.TERCERA, null, 1L);

        assertNull(cosecha.getObservaciones());
    }
}
