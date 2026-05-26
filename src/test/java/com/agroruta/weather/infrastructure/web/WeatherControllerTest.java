package com.agroruta.weather.infrastructure.web;

import com.agroruta.weather.application.port.in.WeatherUseCase;
import com.agroruta.weather.domain.Weather;
import com.agroruta.weather.infrastructure.web.dto.WeatherResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherController - Pruebas Unitarias")
class WeatherControllerTest {

    @Mock
    private WeatherUseCase weatherUseCase;

    @InjectMocks
    private WeatherController weatherController;

    private Weather weatherBase;

    @BeforeEach
    void setUp() {
        weatherBase = new Weather(
                "Bogotá", "CO", 18.5, 17.0, 72, 3.2, "parcialmente nublado", "04d"
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getByCity
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getByCity()")
    class GetByCity {

        @Test
        @DisplayName("Debe retornar 200 y el DTO mapeado correctamente")
        void debeRetornar200ConDtoMapeado() {
            when(weatherUseCase.getWeatherByCity("Bogotá")).thenReturn(weatherBase);

            ResponseEntity<WeatherResponseDto> respuesta = weatherController.getByCity("Bogotá");

            assertNotNull(respuesta);
            assertEquals(HttpStatus.OK,              respuesta.getStatusCode());
            assertNotNull(respuesta.getBody());
            assertEquals("Bogotá",                   respuesta.getBody().getCity());
            assertEquals("CO",                       respuesta.getBody().getCountry());
            assertEquals(18.5,                       respuesta.getBody().getTemperature());
            assertEquals(17.0,                       respuesta.getBody().getFeelsLike());
            assertEquals(72,                         respuesta.getBody().getHumidity());
            assertEquals(3.2,                        respuesta.getBody().getWindSpeed());
            assertEquals("parcialmente nublado",     respuesta.getBody().getDescription());
            verify(weatherUseCase, times(1)).getWeatherByCity("Bogotá");
        }

        @Test
        @DisplayName("Debe construir el iconUrl con el formato correcto de OpenWeatherMap")
        void debeConstruirElIconUrlCorrectamente() {
            when(weatherUseCase.getWeatherByCity("Bogotá")).thenReturn(weatherBase);

            ResponseEntity<WeatherResponseDto> respuesta = weatherController.getByCity("Bogotá");

            assertEquals(
                    "https://openweathermap.org/img/wn/04d@2x.png",
                    respuesta.getBody().getIconUrl()
            );
        }

        @Test
        @DisplayName("Debe delegar al useCase con el nombre de ciudad exacto recibido")
        void debeDelegarAlUseCaseConLaCiudadExacta() {
            Weather weatherMedellin = new Weather(
                    "Medellín", "CO", 24.0, 23.0, 60, 1.5, "soleado", "01d"
            );
            when(weatherUseCase.getWeatherByCity("Medellín")).thenReturn(weatherMedellin);

            ResponseEntity<WeatherResponseDto> respuesta = weatherController.getByCity("Medellín");

            assertEquals(HttpStatus.OK,  respuesta.getStatusCode());
            assertEquals("Medellín",     respuesta.getBody().getCity());
            assertEquals(24.0,           respuesta.getBody().getTemperature());
            verify(weatherUseCase, times(1)).getWeatherByCity("Medellín");
            verify(weatherUseCase, never()).getWeatherByCity("Bogotá");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getByCoordinates
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getByCoordinates()")
    class GetByCoordinates {

        @Test
        @DisplayName("Debe retornar 200 y el DTO mapeado correctamente")
        void debeRetornar200ConDtoMapeado() {
            when(weatherUseCase.getWeatherByCoordinates(4.711, -74.0721)).thenReturn(weatherBase);

            ResponseEntity<WeatherResponseDto> respuesta = weatherController.getByCoordinates(4.711, -74.0721);

            assertNotNull(respuesta);
            assertEquals(HttpStatus.OK,          respuesta.getStatusCode());
            assertNotNull(respuesta.getBody());
            assertEquals("Bogotá",               respuesta.getBody().getCity());
            assertEquals("CO",                   respuesta.getBody().getCountry());
            assertEquals(18.5,                   respuesta.getBody().getTemperature());
            assertEquals(72,                     respuesta.getBody().getHumidity());
            assertEquals("parcialmente nublado", respuesta.getBody().getDescription());
            verify(weatherUseCase, times(1)).getWeatherByCoordinates(4.711, -74.0721);
        }

        @Test
        @DisplayName("Debe construir el iconUrl con el formato correcto de OpenWeatherMap")
        void debeConstruirElIconUrlCorrectamente() {
            when(weatherUseCase.getWeatherByCoordinates(4.711, -74.0721)).thenReturn(weatherBase);

            ResponseEntity<WeatherResponseDto> respuesta = weatherController.getByCoordinates(4.711, -74.0721);

            assertEquals(
                    "https://openweathermap.org/img/wn/04d@2x.png",
                    respuesta.getBody().getIconUrl()
            );
        }

        @Test
        @DisplayName("Debe delegar al useCase con las coordenadas exactas recibidas")
        void debeDelegarAlUseCaseConLasCoordenadasExactas() {
            Weather weatherCali = new Weather(
                    "Cali", "CO", 27.0, 26.0, 55, 2.0, "despejado", "01d"
            );
            when(weatherUseCase.getWeatherByCoordinates(3.4516, -76.5320)).thenReturn(weatherCali);

            ResponseEntity<WeatherResponseDto> respuesta = weatherController.getByCoordinates(3.4516, -76.5320);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals("Cali",        respuesta.getBody().getCity());
            assertEquals(27.0,          respuesta.getBody().getTemperature());
            verify(weatherUseCase, times(1)).getWeatherByCoordinates(3.4516, -76.5320);
            verify(weatherUseCase, never()).getWeatherByCoordinates(4.711, -74.0721);
        }
    }
}