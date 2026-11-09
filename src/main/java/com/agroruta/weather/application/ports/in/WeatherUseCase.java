// com/agroruta/weather/application/ports/in/WeatherUseCase.java
package com.agroruta.weather.application.ports.in;

import com.agroruta.weather.domain.Pronostico;
import com.agroruta.weather.domain.Weather;

public interface WeatherUseCase {
    Weather getWeatherByCity(String city);
    Weather getWeatherByCoordinates(double lat, double lon);
    /** Mínima, máxima, lluvia y viento de las próximas 24 horas */
    Pronostico getPronostico24h(double lat, double lon);
}