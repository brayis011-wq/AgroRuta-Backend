package com.agroruta.weather.infrastructure;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Acceso a OpenWeather.
 *
 * @param key     clave de la API ({@code WEATHER_API_KEY})
 * @param baseUrl dirección base de la API 2.5
 */
@Validated
@ConfigurationProperties("weather.api")
public record ClimaProperties(
        @NotBlank String key, @NotBlank String baseUrl) {}
