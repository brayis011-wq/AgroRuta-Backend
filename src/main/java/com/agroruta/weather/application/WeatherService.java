// com/agroruta/weather/application/WeatherService.java
package com.agroruta.weather.application;

import com.agroruta.weather.application.port.in.WeatherUseCase;
import com.agroruta.weather.application.port.out.WeatherApiPort;
import com.agroruta.weather.domain.Weather;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherService implements WeatherUseCase {

    private final WeatherApiPort weatherApiPort;

    @Override
    public Weather getWeatherByCity(String city) {
        return weatherApiPort.fetchWeatherByCity(city);
    }

    @Override
    public Weather getWeatherByCoordinates(double lat, double lon) {
        return weatherApiPort.fetchWeatherByCoordinates(lat, lon);
    }
}