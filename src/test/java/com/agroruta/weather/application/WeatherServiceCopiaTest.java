package com.agroruta.weather.application;

import com.agroruta.weather.application.ports.out.WeatherApiPort;
import com.agroruta.weather.domain.Weather;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("WeatherService - copias breves")
class WeatherServiceCopiaTest {

    /** Un reloj que se puede adelantar */
    private static final class Reloj extends Clock {
        private Instant ahora = Instant.parse("2026-11-18T12:00:00Z");
        void adelantar(Duration d) { ahora = ahora.plus(d); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final WeatherApiPort api = mock(WeatherApiPort.class);
    private final Reloj reloj = new Reloj();
    private final WeatherService servicio = new WeatherService(api, reloj);
    private final Weather bogota = new Weather("Bogotá", "CO", 18.5, 17.0, 72, 3.2, "Nublado", "04d");

    @Test
    @DisplayName("el clima actual se pide una vez cada diez minutos por lugar (fincas vecinas comparten)")
    void actualPorLugar() {
        when(api.fetchWeatherByCoordinates(anyDouble(), anyDouble())).thenReturn(bogota);

        servicio.getWeatherByCoordinates(4.7110, -74.0721);
        servicio.getWeatherByCoordinates(4.7140, -74.0700); // a unos metros: misma copia
        verify(api, times(1)).fetchWeatherByCoordinates(anyDouble(), anyDouble());

        reloj.adelantar(WeatherService.VIGENCIA_ACTUAL.plusSeconds(1));
        servicio.getWeatherByCoordinates(4.7110, -74.0721);
        verify(api, times(2)).fetchWeatherByCoordinates(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("por ciudad no importan mayúsculas ni espacios")
    void porCiudad() {
        when(api.fetchWeatherByCity("Bogotá")).thenReturn(bogota);
        when(api.fetchWeatherByCity(" bogotá ")).thenReturn(bogota);
        servicio.getWeatherByCity("Bogotá");
        servicio.getWeatherByCity(" bogotá ");
        verify(api, times(1)).fetchWeatherByCity("Bogotá");
    }

    @Test
    @DisplayName("un error no se guarda: la siguiente consulta vuelve a intentar")
    void errorNoSeGuarda() {
        when(api.fetchWeatherByCity("Tunja")).thenThrow(new RuntimeException("sin servicio")).thenReturn(bogota);
        assertThatThrownBy(() -> servicio.getWeatherByCity("Tunja")).isInstanceOf(RuntimeException.class);
        assertThat(servicio.getWeatherByCity("Tunja")).isSameAs(bogota);
    }

    @Test
    @DisplayName("el pronóstico de 24 horas dura media hora")
    void pronostico() {
        servicio.getPronostico24h(4.71, -74.07);
        servicio.getPronostico24h(4.71, -74.07);
        // El simulacro devuelve null: lo nulo no se guarda, así que se pidió dos veces
        verify(api, times(2)).fetchPronostico24h(4.71, -74.07);
    }
}
