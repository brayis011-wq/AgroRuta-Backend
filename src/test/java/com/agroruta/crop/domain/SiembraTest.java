package com.agroruta.crop.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class SiembraTest {

    @Test
    void deberiaCrearSiembraConConstructorCompleto() {
        LocalDate fecha = LocalDate.of(2026, 1, 15);

        Siembra siembra = new Siembra(1L, fecha, 500, VariedadUchuva.COLOMBIA, 2L);

        assertEquals(1L, siembra.getId());
        assertEquals(fecha, siembra.getFechaSiembra());
        assertEquals(500, siembra.getCantidadPlantas());
        assertEquals(VariedadUchuva.COLOMBIA, siembra.getVariedad());
        assertEquals(2L, siembra.getLoteId());
        assertEquals(EstadoCultivo.GERMINACION, siembra.getEstadoCultivo());
    }

    @Test
    void deberiaIniciarEnGerminacion() {
        Siembra siembra = new Siembra(1L, LocalDate.now(), 100, VariedadUchuva.COLOMBIA, 1L);

        assertEquals(EstadoCultivo.GERMINACION, siembra.getEstadoCultivo());
    }

    @Test
    void deberiaAvanzarDeGerminacionACrecimiento() {
        Siembra siembra = new Siembra(1L, LocalDate.now(), 100, VariedadUchuva.COLOMBIA, 1L);

        siembra.avanzarEtapa();

        assertEquals(EstadoCultivo.CRECIMIENTO, siembra.getEstadoCultivo());
    }

    @Test
    void deberiaAvanzarTodasLasEtapas() {
        Siembra siembra = new Siembra(1L, LocalDate.now(), 100, VariedadUchuva.KENYA, 1L);

        siembra.avanzarEtapa();
        assertEquals(EstadoCultivo.CRECIMIENTO, siembra.getEstadoCultivo());

        siembra.avanzarEtapa();
        assertEquals(EstadoCultivo.PRODUCCION, siembra.getEstadoCultivo());

        siembra.avanzarEtapa();
        assertEquals(EstadoCultivo.COSECHA, siembra.getEstadoCultivo());

        siembra.avanzarEtapa();
        assertEquals(EstadoCultivo.FINALIZADO, siembra.getEstadoCultivo());
    }

    @Test
    void deberiaLanzarExcepcionSiYaEstaFinalizado() {
        Siembra siembra = new Siembra(1L, LocalDate.now(), 100, VariedadUchuva.GIGANTE, 1L);

        siembra.avanzarEtapa(); // CRECIMIENTO
        siembra.avanzarEtapa(); // PRODUCCION
        siembra.avanzarEtapa(); // COSECHA
        siembra.avanzarEtapa(); // FINALIZADO

        assertThrows(IllegalStateException.class, () -> siembra.avanzarEtapa());
    }

    @Test
    void deberiaCrearSiembraVaciaConConstructorPorDefecto() {
        Siembra siembra = new Siembra();

        assertNull(siembra.getId());
        assertNull(siembra.getEstadoCultivo());
    }

    @Test
    void deberiaSoportarTodasLasVariedades() {
        for (VariedadUchuva variedad : VariedadUchuva.values()) {
            Siembra siembra = new Siembra(1L, LocalDate.now(), 100, variedad, 1L);

            assertEquals(variedad, siembra.getVariedad());
        }
    }
}