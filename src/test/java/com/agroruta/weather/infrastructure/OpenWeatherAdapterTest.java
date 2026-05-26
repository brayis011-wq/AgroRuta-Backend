package com.agroruta.weather.infrastructure;

import com.agroruta.shared.exception.ExternalApiException;
import com.agroruta.weather.domain.Weather;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenWeatherAdapter - Pruebas Unitarias")
class OpenWeatherAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OpenWeatherAdapter adapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "apiKey",  "test-api-key");
        ReflectionTestUtils.setField(adapter, "baseUrl", "https://api.openweathermap.org/data/2.5");
    }

    // Construye un Map que imita la respuesta real de OpenWeatherMap
    private Map<String, Object> buildApiResponse(String city, String country,
                                                 double temp, double feelsLike,
                                                 int humidity, double windSpeed,
                                                 String description, String icon) {
        return Map.of(
                "name",    city,
                "sys",     Map.of("country", country),
                "main",    Map.of("temp", temp, "feels_like", feelsLike, "humidity", humidity),
                "wind",    Map.of("speed", windSpeed),
                "weather", List.of(Map.of("description", description, "icon", icon))
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    //  fetchWeatherByCity
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("fetchWeatherByCity()")
    class FetchWeatherByCity {

        @Test
        @DisplayName("Debe mapear correctamente todos los campos del Weather cuando la API responde")
        void debeMappearTodosLosCamposDelWeather() {
            Map<String, Object> apiResponse = buildApiResponse(
                    "Bogotá", "CO", 18.5, 17.0, 72, 3.2, "parcialmente nublado", "04d"
            );
            when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

            Weather resultado = adapter.fetchWeatherByCity("Bogotá");

            assertNotNull(resultado);
            assertEquals("Bogotá",                resultado.getCity());
            assertEquals("CO",                    resultado.getCountry());
            assertEquals(18.5,                    resultado.getTemperature());
            assertEquals(17.0,                    resultado.getFeelsLike());
            assertEquals(72,                      resultado.getHumidity());
            assertEquals(3.2,                     resultado.getWindSpeed());
            assertEquals("parcialmente nublado",  resultado.getDescription());
            assertEquals("04d",                   resultado.getIcon());
        }

        @Test
        @DisplayName("Debe construir la URL correcta con city, apiKey y units=metric")
        void debeConstruirLaUrlCorrectaParaCity() {
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            Map<String, Object> apiResponse = buildApiResponse(
                    "Bogotá", "CO", 18.5, 17.0, 72, 3.2, "soleado", "01d"
            );
            when(restTemplate.getForObject(urlCaptor.capture(), eq(Map.class))).thenReturn(apiResponse);

            adapter.fetchWeatherByCity("Bogotá");

            String urlUsada = urlCaptor.getValue();
            assertTrue(urlUsada.contains("Bogotá"));
            assertTrue(urlUsada.contains("test-api-key"));
            assertTrue(urlUsada.contains("units=metric"));
            assertTrue(urlUsada.contains("lang=es"));
            assertTrue(urlUsada.startsWith("https://api.openweathermap.org/data/2.5"));
        }

        @Test
        @DisplayName("Debe lanzar ExternalApiException cuando la API retorna null")
        void debeLanzarExceptionSiApiRetornaNull() {
            when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

            ExternalApiException ex = assertThrows(
                    ExternalApiException.class,
                    () -> adapter.fetchWeatherByCity("Bogotá")
            );

            assertEquals("Respuesta vacía de OpenWeatherMap", ex.getMessage());
        }

        @Test
        @DisplayName("Debe lanzar ExternalApiException cuando el RestTemplate lanza una excepción")
        void debeLanzarExceptionSiRestTemplateFalla() {
            when(restTemplate.getForObject(anyString(), eq(Map.class)))
                    .thenThrow(new RuntimeException("Timeout"));

            ExternalApiException ex = assertThrows(
                    ExternalApiException.class,
                    () -> adapter.fetchWeatherByCity("Bogotá")
            );

            assertTrue(ex.getMessage().contains("Error al consultar OpenWeatherMap"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  fetchWeatherByCoordinates
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("fetchWeatherByCoordinates()")
    class FetchWeatherByCoordinates {

        @Test
        @DisplayName("Debe mapear correctamente todos los campos del Weather cuando la API responde")
        void debeMappearTodosLosCamposDelWeather() {
            Map<String, Object> apiResponse = buildApiResponse(
                    "Bogotá", "CO", 18.5, 17.0, 72, 3.2, "nublado", "03d"
            );
            when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

            Weather resultado = adapter.fetchWeatherByCoordinates(4.711, -74.0721);

            assertNotNull(resultado);
            assertEquals("Bogotá", resultado.getCity());
            assertEquals("CO",     resultado.getCountry());
            assertEquals(18.5,     resultado.getTemperature());
            assertEquals(72,       resultado.getHumidity());
        }

        @Test
        @DisplayName("Debe construir la URL correcta con lat, lon, apiKey y units=metric")
        void debeConstruirLaUrlCorrectaParaCoordenadas() {
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            Map<String, Object> apiResponse = buildApiResponse(
                    "Bogotá", "CO", 18.5, 17.0, 72, 3.2, "soleado", "01d"
            );
            when(restTemplate.getForObject(urlCaptor.capture(), eq(Map.class))).thenReturn(apiResponse);

            adapter.fetchWeatherByCoordinates(4.711, -74.0721);

            String urlUsada = urlCaptor.getValue();
            assertTrue(urlUsada.contains("4.711"));
            assertTrue(urlUsada.contains("-74.0721"));
            assertTrue(urlUsada.contains("test-api-key"));
            assertTrue(urlUsada.contains("units=metric"));
            assertTrue(urlUsada.contains("lang=es"));
            assertTrue(urlUsada.startsWith("https://api.openweathermap.org/data/2.5"));
        }

        @Test
        @DisplayName("Debe lanzar ExternalApiException cuando la API retorna null")
        void debeLanzarExceptionSiApiRetornaNull() {
            when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

            ExternalApiException ex = assertThrows(
                    ExternalApiException.class,
                    () -> adapter.fetchWeatherByCoordinates(4.711, -74.0721)
            );

            assertEquals("Respuesta vacía de OpenWeatherMap", ex.getMessage());
        }

        @Test
        @DisplayName("Debe lanzar ExternalApiException cuando el RestTemplate lanza una excepción")
        void debeLanzarExceptionSiRestTemplateFalla() {
            when(restTemplate.getForObject(anyString(), eq(Map.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            ExternalApiException ex = assertThrows(
                    ExternalApiException.class,
                    () -> adapter.fetchWeatherByCoordinates(4.711, -74.0721)
            );

            assertTrue(ex.getMessage().contains("Error al consultar OpenWeatherMap"));
        }
    }
}