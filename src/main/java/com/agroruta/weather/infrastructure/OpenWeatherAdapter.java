package com.agroruta.weather.infrastructure.external;

import com.agroruta.weather.application.port.out.WeatherApiPort;
import com.agroruta.weather.domain.Weather;
import com.agroruta.shared.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenWeatherAdapter implements WeatherApiPort {

    private final RestTemplate restTemplate;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Override
    public Weather fetchWeatherByCity(String city) {
        String url = String.format("%s/weather?q=%s&appid=%s&units=metric&lang=es",
                baseUrl, city, apiKey);
        return callApi(url);
    }

    @Override
    public Weather fetchWeatherByCoordinates(double lat, double lon) {
        String url = String.format("%s/weather?lat=%s&lon=%s&appid=%s&units=metric&lang=es",
                baseUrl, lat, lon, apiKey);
        return callApi(url);
    }

    @SuppressWarnings("unchecked")
    private Weather callApi(String url) {
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) throw new ExternalApiException("Respuesta vacía de OpenWeatherMap");

            Map<String, Object> main    = (Map<String, Object>) response.get("main");
            Map<String, Object> wind    = (Map<String, Object>) response.get("wind");
            Map<String, Object> sys     = (Map<String, Object>) response.get("sys");
            List<Map<String, Object>> weatherList = (List<Map<String, Object>>) response.get("weather");
            Map<String, Object> weatherInfo = weatherList.get(0);

            Weather weather = new Weather();
            weather.setCity((String) response.get("name"));
            weather.setCountry((String) sys.get("country"));
            weather.setTemperature(((Number) main.get("temp")).doubleValue());
            weather.setFeelsLike(((Number) main.get("feels_like")).doubleValue());
            weather.setHumidity(((Number) main.get("humidity")).intValue());
            weather.setWindSpeed(((Number) wind.get("speed")).doubleValue());
            weather.setDescription((String) weatherInfo.get("description"));
            weather.setIcon((String) weatherInfo.get("icon"));

            return weather;
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalApiException("Error al consultar OpenWeatherMap: " + e.getMessage());
        }
    }
}