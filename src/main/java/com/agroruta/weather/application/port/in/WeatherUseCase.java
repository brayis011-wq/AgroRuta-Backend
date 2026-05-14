// com/agroruta/weather/application/ports/in/WeatherUseCase.java
package com.agroruta.weather.application.ports.in;

import com.agroruta.weather.domain.Weather;

public interface WeatherUseCase {
    Weather getWeatherByCity(String city);
    Weather getWeatherByCoordinates(double lat, double lon);
}