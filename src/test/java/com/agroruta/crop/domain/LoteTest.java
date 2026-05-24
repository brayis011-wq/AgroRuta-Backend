package com.agroruta.crop.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoteTest {

    @Test
    void deberiaCrearLoteConConstructorCompleto() {
        Lote lote = new Lote(1L, "Lote A", 10.0, 5L);

        assertEquals(1L, lote.getId());
        assertEquals("Lote A", lote.getNombre());
        assertEquals(10.0, lote.getArea());
        assertEquals(5L, lote.getFincaId());
        assertEquals(EstadoLote.DISPONIBLE, lote.getEstado());
    }

    @Test
    void deberiaCrearLoteVacioConConstructorPorDefecto() {
        Lote lote = new Lote();

        assertNull(lote.getId());
        assertNull(lote.getNombre());
        assertNull(lote.getEstado());
    }

    @Test
    void deberiaIniciarCultivoCuandoEstaDisponible() {
        Lote lote = new Lote(1L, "Lote A", 10.0, 5L);

        lote.iniciarCultivo();

        assertEquals(EstadoLote.EN_CULTIVO, lote.getEstado());
    }

    @Test
    void deberiaLanzarExcepcionAlIniciarCultivoSiNoEstaDisponible() {
        Lote lote = new Lote(1L, "Lote A", 10.0, 5L);
        lote.iniciarCultivo();

        assertThrows(IllegalStateException.class, () -> lote.iniciarCultivo());
    }

    @Test
    void deberiaPasarADescanso() {
        Lote lote = new Lote(1L, "Lote A", 10.0, 5L);

        lote.ponerEnDescanso();

        assertEquals(EstadoLote.EN_DESCANSO, lote.getEstado());
    }

    @Test
    void deberiaDisponibilizar() {
        Lote lote = new Lote(1L, "Lote A", 10.0, 5L);
        lote.ponerEnDescanso();

        lote.disponibilizar();

        assertEquals(EstadoLote.DISPONIBLE, lote.getEstado());
    }

    @Test
    void deberiaActualizarGeometria() {
        Lote lote = new Lote(1L, "Lote A", 10.0, 5L);

        lote.actualizarGeometria("[[4.71,-74.07]]", 15.0, 4.71, -74.07);

        assertEquals("[[4.71,-74.07]]", lote.getCoordenadas());
        assertEquals(15.0, lote.getArea());
        assertEquals(4.71, lote.getCentroideLat());
        assertEquals(-74.07, lote.getCentroideLng());
    }

    @Test
    void deberiaLanzarExcepcionSiCoordenadasSonNulas() {
        Lote lote = new Lote(1L, "Lote A", 10.0, 5L);

        assertThrows(IllegalArgumentException.class,
                () -> lote.actualizarGeometria(null, 15.0, 4.71, -74.07));
    }

    @Test
    void deberiaLanzarExcepcionSiCoordenadasEstanVacias() {
        Lote lote = new Lote(1L, "Lote A", 10.0, 5L);

        assertThrows(IllegalArgumentException.class,
                () -> lote.actualizarGeometria("   ", 15.0, 4.71, -74.07));
    }

    @Test
    void deberiaTenerGeometriaCuandoCoordenadasEstanPresentes() {
        Lote lote = new Lote(1L, "Lote A", 10.0, 5L);
        lote.actualizarGeometria("[[4.71,-74.07]]", 15.0, 4.71, -74.07);

        assertTrue(lote.tieneGeometria());
    }

    @Test
    void noDeberiaTenerGeometriaCuandoCoordenadasSonNulas() {
        Lote lote = new Lote(1L, "Lote A", 10.0, 5L);

        assertFalse(lote.tieneGeometria());
    }
}