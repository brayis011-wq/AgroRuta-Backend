package com.agroruta.weather.infrastructure.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeatherResponseDto {
    private String city;
    private String country;
    private double temperature;
    private double feelsLike;
    private int humidity;
    private double windSpeed;
    private String description;
    private String iconUrl;
}