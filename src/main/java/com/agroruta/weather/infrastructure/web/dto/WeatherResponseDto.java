package com.agroruta.weather.infrastructure.web.dto;

import lombok.Builder;

@Builder
public record WeatherResponseDto(
        String city,
        String country,
        double temperature,
        double feelsLike,
        int humidity,
        double windSpeed,
        String description,
        String iconUrl) {}
