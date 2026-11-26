package com.agroruta.weather.domain;

/** Clima actual de un lugar, tal como lo entrega OpenWeather (temperaturas en °C, viento en m/s). */
public record Weather(
        String city,
        String country,
        double temperature,
        double feelsLike,
        int humidity,
        double windSpeed,
        String description,
        String icon) {}
