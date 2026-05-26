package com.agroruta.weather.application;

import com.agroruta.weather.application.port.out.WeatherApiPort;
import com.agroruta.weather.domain.Weather;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService - Pruebas Unitarias")
class WeatherServiceTest {

    @Mock
    private WeatherApiPort weatherApiPort;

    @InjectMocks
    private WeatherService weatherService;

    private Weather weatherBase;

    @BeforeEach
    void setUp() {
        weatherBase = new Weather("Bogotá", "CO", 18.5, 17.0, 72, 3.2, "Parcialmente nublado", "04d");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getWeatherByCity
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getWeatherByCity()")
    class GetWeatherByCity {

        @Test
        @DisplayName("Debe retornar el clima de la ciudad delegando al puerto")
        void debeRetornarElClimaDeLaCiudad() {
            when(weatherApiPort.fetchWeatherByCity("Bogotá")).thenReturn(weatherBase);

            Weather resultado = weatherService.getWeatherByCity("Bogotá");

            assertNotNull(resultado);
            assertEquals("Bogotá",               resultado.getCity());
            assertEquals(18.5,                   resultado.getTemperature());
            assertEquals("Parcialmente nublado", resultado.getDescription());
            verify(weatherApiPort, times(1)).fetchWeatherByCity("Bogotá");
        }

        @Test
        @DisplayName("Debe delegar al puerto con el nombre de ciudad exacto recibido")
        void debeDelegarAlPuertoConElNombreDeCiudadExacto() {
            Weather weatherMedellin = new Weather("Medellín", "CO", 24.0, 23.0, 60, 1.5, "Soleado", "01d");
            when(weatherApiPort.fetchWeatherByCity("Medellín")).thenReturn(weatherMedellin);

            Weather resultado = weatherService.getWeatherByCity("Medellín");

            assertNotNull(resultado);
            assertEquals("Medellín", resultado.getCity());
            assertEquals(24.0,       resultado.getTemperature());
            verify(weatherApiPort, times(1)).fetchWeatherByCity("Medellín");
            verify(weatherApiPort, never()).fetchWeatherByCity("Bogotá");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getWeatherByCoordinates
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getWeatherByCoordinates()")
    class GetWeatherByCoordinates {

        @Test
        @DisplayName("Debe retornar el clima de las coordenadas delegando al puerto")
        void debeRetornarElClimaDeLasCoordenadas() {
            when(weatherApiPort.fetchWeatherByCoordinates(4.711, -74.0721)).thenReturn(weatherBase);

            Weather resultado = weatherService.getWeatherByCoordinates(4.711, -74.0721);

            assertNotNull(resultado);
            assertEquals("Bogotá", resultado.getCity());
            assertEquals(18.5,     resultado.getTemperature());
            verify(weatherApiPort, times(1)).fetchWeatherByCoordinates(4.711, -74.0721);
        }

        @Test
        @DisplayName("Debe delegar al puerto con las coordenadas exactas recibidas")
        void debeDelegarAlPuertoConLasCoordenadasExactas() {
            Weather weatherCali = new Weather("Cali", "CO", 27.0, 26.0, 55, 2.0, "Despejado", "01d");
            when(weatherApiPort.fetchWeatherByCoordinates(3.4516, -76.5320)).thenReturn(weatherCali);

            Weather resultado = weatherService.getWeatherByCoordinates(3.4516, -76.5320);

            assertNotNull(resultado);
            assertEquals("Cali", resultado.getCity());
            assertEquals(27.0,   resultado.getTemperature());
            verify(weatherApiPort, times(1)).fetchWeatherByCoordinates(3.4516, -76.5320);
            verify(weatherApiPort, never()).fetchWeatherByCoordinates(4.711, -74.0721);
        }
    }
}