package com.agroruta.notification.application;

import com.agroruta.crop.application.ports.in.AlertasCultivoUseCase;
import com.agroruta.crop.application.ports.out.FincaRepository;
import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.crop.domain.Finca;
import com.agroruta.shared.exception.ExternalApiException;
import com.agroruta.weather.application.ports.in.WeatherUseCase;
import com.agroruta.weather.domain.ConsejoClima;
import com.agroruta.weather.domain.ConsejoClima.Nivel;
import com.agroruta.weather.domain.ConsejoClima.Tipo;
import com.agroruta.weather.domain.FranjaClima;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsejosClimaService")
class ConsejosClimaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);

    @Mock private FincaRepository fincas;
    @Mock private AlertasCultivoUseCase alertas;
    @Mock private WeatherUseCase clima;

    private ConsejosClimaService service;
    private Finca finca;

    @BeforeEach
    void preparar() {
        Clock seisAm = Clock.fixed(HOY.atTime(6, 0).atZone(ZONA).toInstant(), ZONA);
        service = new ConsejosClimaService(fincas, alertas, clima, seisAm);
        finca = mock(Finca.class);
        lenient().when(finca.getId()).thenReturn(3L);
        lenient().when(finca.getNombre()).thenReturn("La Esperanza");
        lenient().when(finca.tieneCentroide()).thenReturn(true);
        lenient().when(finca.getCentroideLat()).thenReturn(4.6);
        lenient().when(finca.getCentroideLng()).thenReturn(-74.1);
        lenient().when(fincas.findByAgricultorId(9L)).thenReturn(List.of(finca));
    }

    private static List<FranjaClima> franjas(double lluviaTarde, double tempMadrugada) {
        List<FranjaClima> f = new ArrayList<>();
        for (int d = 0; d < 2; d++) {
            for (int h = 1; h < 24; h += 3) {
                LocalDateTime t = HOY.plusDays(d).atTime(h, 0);
                boolean tarde = d == 0 && h == 16;
                f.add(new FranjaClima(t, h == 4 ? tempMadrugada : 18, tarde ? lluviaTarde : 0, tarde && lluviaTarde > 0 ? 0.9 : 0.1, 2));
            }
        }
        return f;
    }

    private static AlertaCultivo alerta(AlertaCultivo.Tipo tipo, String titulo) {
        return new AlertaCultivo(tipo, "La Esperanza", "Lote El Mango", "Café · Castillo", titulo, null, HOY);
    }

    @Test
    @DisplayName("hoy toca fumigar y llueve en la tarde: aconseja fumigar temprano")
    void fumigarTemprano() {
        when(alertas.alertasDelDia(9L, HOY)).thenReturn(List.of(alerta(AlertaCultivo.Tipo.FUMIGACION_HOY, null)));
        when(clima.getFranjas48h(4.6, -74.1)).thenReturn(franjas(6, 12));

        List<ConsejoClima> c = service.consejos(9L);

        assertEquals(1, c.size());
        assertEquals(Tipo.FUMIGAR_EN_VENTANA, c.get(0).tipo());
        assertEquals(Nivel.PRECAUCION, c.get(0).nivel());
        assertEquals("Lote El Mango", c.get(0).lote());
        assertEquals("LLUVIA", c.get(0).motivo());
        assertEquals(HOY.atTime(16, 0), c.get(0).problemaDesde());
        assertEquals(HOY.atTime(13, 0), c.get(0).ventanaFin());
    }

    @Test
    @DisplayName("hoy toca fumigar y el día está bueno: lo dice")
    void buenDia() {
        when(alertas.alertasDelDia(9L, HOY)).thenReturn(List.of(alerta(AlertaCultivo.Tipo.FUMIGACION_ATRASADA, null)));
        when(clima.getFranjas48h(4.6, -74.1)).thenReturn(franjas(0, 12));

        List<ConsejoClima> c = service.consejos(9L);

        assertEquals(1, c.size());
        assertEquals(Tipo.FUMIGAR_BIEN, c.get(0).tipo());
        assertEquals(Nivel.BIEN, c.get(0).nivel());
    }

    @Test
    @DisplayName("sin tareas no opina de fumigación, pero sí avisa helada y lluvia fuerte; lo grave va primero")
    void avisosDeLaFinca() {
        when(alertas.alertasDelDia(9L, HOY)).thenReturn(List.of());
        when(clima.getFranjas48h(4.6, -74.1)).thenReturn(franjas(25, 1.5));

        List<ConsejoClima> c = service.consejos(9L);

        assertEquals(2, c.size());
        assertEquals(Tipo.HELADA, c.get(0).tipo());
        assertEquals(1.5, c.get(0).valor());
        assertEquals(Tipo.LLUVIA_FUERTE, c.get(1).tipo());
        assertEquals(25.0, c.get(1).valor());
        assertNull(c.get(0).lote());
    }

    @Test
    @DisplayName("riego programado y va a llover: no hace falta regar")
    void noRegar() {
        when(alertas.alertasDelDia(9L, HOY)).thenReturn(List.of(alerta(AlertaCultivo.Tipo.LABOR_HOY, "RIEGO")));
        when(clima.getFranjas48h(4.6, -74.1)).thenReturn(franjas(8, 12));

        List<ConsejoClima> c = service.consejos(9L);

        assertEquals(1, c.size());
        assertEquals(Tipo.NO_REGAR, c.get(0).tipo());
        assertEquals(8.0, c.get(0).valor());
    }

    @Test
    @DisplayName("una finca sin ubicación o sin pronóstico no rompe nada")
    void sinPronostico() {
        when(alertas.alertasDelDia(9L, HOY)).thenReturn(List.of(alerta(AlertaCultivo.Tipo.FUMIGACION_HOY, null)));
        when(clima.getFranjas48h(anyDouble(), anyDouble())).thenThrow(new ExternalApiException("caído"));
        assertTrue(service.consejos(9L).isEmpty());

        when(finca.tieneCentroide()).thenReturn(false);
        assertTrue(service.consejos(9L).isEmpty());
        assertTrue(service.consejos(null).isEmpty());
    }

    @Test
    @DisplayName("las tareas de otra finca no se cruzan con el clima de esta")
    void otraFinca() {
        when(alertas.alertasDelDia(9L, HOY)).thenReturn(List.of(
                new AlertaCultivo(AlertaCultivo.Tipo.FUMIGACION_HOY, "Otra finca", "Lote 1", "Café", null, null, HOY)));
        when(clima.getFranjas48h(4.6, -74.1)).thenReturn(franjas(6, 12));
        assertTrue(service.consejos(9L).isEmpty());
    }
}
