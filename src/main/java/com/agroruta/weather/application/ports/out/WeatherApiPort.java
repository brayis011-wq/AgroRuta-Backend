// com/agroruta/weather/application/ports/out/WeatherApiPort.java
package com.agroruta.weather.application.ports.out;

import com.agroruta.weather.domain.Weather;

public interface WeatherApiPort {
    Weather fetchWeatherByCity(String city);
    Weather fetchWeatherByCoordinates(double lat, double lon);
    com.agroruta.weather.domain.Pronostico fetchPronostico24h(double lat, double lon);
}