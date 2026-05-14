package com.agroruta.weather.infrastructure.web;

import com.agroruta.weather.application.ports.in.WeatherUseCase;
import com.agroruta.weather.domain.Weather;
import com.agroruta.weather.infrastructure.web.dto.WeatherResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/weather")
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
                .city(w.getCity())
                .country(w.getCountry())
                .temperature(w.getTemperature())
                .feelsLike(w.getFeelsLike())
                .humidity(w.getHumidity())
                .windSpeed(w.getWindSpeed())
                .description(w.getDescription())
                .iconUrl("https://openweathermap.org/img/wn/" + w.getIcon() + "@2x.png")
                .build();
    }
}