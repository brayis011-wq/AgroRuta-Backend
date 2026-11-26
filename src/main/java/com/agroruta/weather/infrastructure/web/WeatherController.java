package com.agroruta.weather.infrastructure.web;

import com.agroruta.weather.application.ports.in.WeatherUseCase;
import com.agroruta.weather.domain.Weather;
import com.agroruta.weather.infrastructure.web.dto.WeatherResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherUseCase weatherUseCase;

    @GetMapping("/city")
    public ResponseEntity<WeatherResponseDto> getByCity(@RequestParam String city) {
        Weather weather = weatherUseCase.getWeatherByCity(city);
        return ResponseEntity.ok(toDto(weather));
    }

    @GetMapping("/coordinates")
    public ResponseEntity<WeatherResponseDto> getByCoordinates(
            @RequestParam double lat,
            @RequestParam double lon) {
        Weather weather = weatherUseCase.getWeatherByCoordinates(lat, lon);
        return ResponseEntity.ok(toDto(weather));
    }

    private WeatherResponseDto toDto(Weather w) {
        return WeatherResponseDto.builder()
                .city(w.city())
                .country(w.country())
                .temperature(w.temperature())
                .feelsLike(w.feelsLike())
                .humidity(w.humidity())
                .windSpeed(w.windSpeed())
                .description(w.description())
                .iconUrl("https://openweathermap.org/img/wn/" + w.icon() + "@2x.png")
                .build();
    }
}