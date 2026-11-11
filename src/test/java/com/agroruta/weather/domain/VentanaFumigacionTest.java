package com.agroruta.weather.domain;

import com.agroruta.weather.domain.VentanaFumigacion.Estado;
import com.agroruta.weather.domain.VentanaFumigacion.Motivo;
import com.agroruta.weather.domain.VentanaFumigacion.Resultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VentanaFumigacion")
class VentanaFumigacionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final LocalDateTime SEIS_AM = HOY.atTime(6, 0);

    /** Dos días de franjas secas y sin viento (1, 4, 7, 10, 13, 16, 19 y 22 horas, como OpenWeather en Colombia) */
    private static List<FranjaClima> secas() {
        List<FranjaClima> f = new ArrayList<>();
        for (int d = 0; d < 2; d++) {
            for (int h = 1; h < 24; h += 3) f.add(new FranjaClima(HOY.plusDays(d).atTime(h, 0), 18, 0, 0.1, 2));
        }
        return f;
    }

    private static void poner(List<FranjaClima> f, LocalDateTime inicio, double lluvia, double viento) {
        for (int i = 0; i < f.size(); i++) {
            if (f.get(i).inicio().equals(inicio)) f.set(i, new FranjaClima(inicio, 18, lluvia, lluvia > 0 ? 0.9 : 0.1, viento));
        }
    }

    @Test
    @DisplayName("día seco y sin viento: se puede fumigar todo el día")
    void diaBueno() {
        Resultado r = VentanaFumigacion.para(secas(), SEIS_AM, HOY).orElseThrow();
        assertEquals(Estado.BUENO, r.estado());
        assertNull(r.motivo());
        assertEquals(HOY.atTime(7, 0), r.ventanaInicio());
        assertEquals(HOY.atTime(19, 0), r.ventanaFin());
    }

    @Test
    @DisplayName("lluvia a las 4 p. m.: hay que fumigar antes de la 1, para que el producto alcance a secar")
    void lluviaEnLaTarde() {
        List<FranjaClima> f = secas();
        poner(f, HOY.atTime(16, 0), 4, 2);
        Resultado r = VentanaFumigacion.para(f, SEIS_AM, HOY).orElseThrow();
        assertEquals(Estado.VENTANA, r.estado());
        assertEquals(Motivo.LLUVIA, r.motivo());
        assertEquals(HOY.atTime(16, 0), r.problemaDesde());
        assertEquals(HOY.atTime(7, 0), r.ventanaInicio());
        assertEquals(HOY.atTime(13, 0), r.ventanaFin());
    }

    @Test
    @DisplayName("lluvia todo el día: no fumigar, y propone la primera ventana de mañana")
    void lluviaTodoElDia() {
        List<FranjaClima> f = secas();
        for (int h = 7; h <= 19; h += 3) poner(f, HOY.atTime(h, 0), 3, 2);
        Resultado r = VentanaFumigacion.para(f, SEIS_AM, HOY).orElseThrow();
        assertEquals(Estado.NO, r.estado());
        assertEquals(Motivo.LLUVIA, r.motivo());
        assertEquals(HOY.atTime(7, 0), r.problemaDesde());
        assertEquals(HOY.plusDays(1).atTime(7, 0), r.ventanaInicio());
        assertEquals(HOY.plusDays(1).atTime(19, 0), r.ventanaFin());
    }

    @Test
    @DisplayName("viento fuerte al mediodía: la ventana es temprano y el motivo es el viento")
    void vientoAlMediodia() {
        List<FranjaClima> f = secas();
        poner(f, HOY.atTime(10, 0), 0, 7);
        poner(f, HOY.atTime(13, 0), 0, 7);
        poner(f, HOY.atTime(16, 0), 0, 7);
        Resultado r = VentanaFumigacion.para(f, SEIS_AM, HOY).orElseThrow();
        assertEquals(Estado.VENTANA, r.estado());
        assertEquals(Motivo.VIENTO, r.motivo());
        assertEquals(HOY.atTime(10, 0), r.problemaDesde());
        assertEquals(HOY.atTime(10, 0), r.ventanaFin());
    }

    @Test
    @DisplayName("si ya va la mañana, la ventana empieza ahora y no en una hora que ya pasó")
    void laVentanaEmpiezaAhora() {
        Resultado r = VentanaFumigacion.para(secas(), HOY.atTime(8, 30), HOY).orElseThrow();
        assertEquals(HOY.atTime(8, 30), r.ventanaInicio());
    }

    @Test
    @DisplayName("de noche ya no hay nada que decir de hoy")
    void deNoche() {
        assertTrue(VentanaFumigacion.para(secas(), HOY.atTime(20, 0), HOY).isEmpty());
        assertTrue(VentanaFumigacion.para(secas(), HOY.atTime(20, 0), HOY.plusDays(1)).isPresent());
    }

    @Test
    @DisplayName("sin ninguna ventana en el pronóstico: no fumigar y sin propuesta")
    void sinVentana() {
        List<FranjaClima> f = secas();
        for (int i = 0; i < f.size(); i++) f.set(i, new FranjaClima(f.get(i).inicio(), 18, 5, 0.9, 2));
        Resultado r = VentanaFumigacion.para(f, SEIS_AM, HOY).orElseThrow();
        assertEquals(Estado.NO, r.estado());
        assertNull(r.ventanaInicio());
    }

    @Test
    @DisplayName("una probabilidad alta de lluvia cuenta como lluvia aunque el pronóstico no dé milímetros")
    void probabilidadAlta() {
        assertTrue(new FranjaClima(SEIS_AM, 18, 0, 0.7, 1).llueve());
        assertFalse(new FranjaClima(SEIS_AM, 18, 0.2, 0.3, 1).llueve());
    }
}
