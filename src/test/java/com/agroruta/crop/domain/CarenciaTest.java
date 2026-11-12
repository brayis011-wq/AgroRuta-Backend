package com.agroruta.crop.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CarenciaTest {

    private static Fumigacion fumigacion(String producto, LocalDate fecha, Integer dias) {
        Fumigacion f = Fumigacion.fromManualEntry(fecha, producto, 1.0, UnidadMedida.LITROS, 1.0, null, 1L);
        f.fijarRestricciones(new RestriccionesFumigacion(dias, null));
        return f;
    }

    private final LocalDate aplicado = LocalDate.of(2026, 11, 1);

    @Test
    void bloqueaDentroDeLosDiasDeCarencia() {
        var b = Carencia.queBloquea(List.of(fumigacion("Mancozeb", aplicado, 14)), aplicado.plusDays(5));
        assertTrue(b.isPresent());
        assertEquals("Mancozeb", b.get().producto());
        assertEquals(LocalDate.of(2026, 11, 15), b.get().libreDesde());
    }

    @Test
    void elDiaQueTerminaLaCarenciaYaSePuedeCosechar() {
        assertTrue(Carencia.queBloquea(List.of(fumigacion("Mancozeb", aplicado, 14)), aplicado.plusDays(14)).isEmpty());
        assertTrue(Carencia.queBloquea(List.of(fumigacion("Mancozeb", aplicado, 14)), aplicado.plusDays(13)).isPresent());
    }

    @Test
    void elMismoDiaDeLaAplicacionEstaBloqueado() {
        assertTrue(Carencia.queBloquea(List.of(fumigacion("Mancozeb", aplicado, 3)), aplicado).isPresent());
    }

    @Test
    void unaFumigacionPosteriorALaCosechaNoCuenta() {
        assertTrue(Carencia.queBloquea(List.of(fumigacion("Mancozeb", aplicado, 14)), aplicado.minusDays(1)).isEmpty());
    }

    @Test
    void sinDiasDeCarenciaNoBloquea() {
        assertTrue(Carencia.queBloquea(List.of(fumigacion("Agua", aplicado, null), fumigacion("Jabón", aplicado, 0)),
                aplicado.plusDays(1)).isEmpty());
    }

    @Test
    void conVariasEligeLaQueTerminaMasTarde() {
        var b = Carencia.queBloquea(List.of(
                fumigacion("Corto", aplicado.plusDays(2), 3),
                fumigacion("Largo", aplicado, 21)), aplicado.plusDays(3));
        assertEquals("Largo", b.orElseThrow().producto());
    }
}
