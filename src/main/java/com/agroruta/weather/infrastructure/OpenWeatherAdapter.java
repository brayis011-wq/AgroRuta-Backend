package com.agroruta.weather.infrastructure;

import com.agroruta.shared.util.ZonaHoraria;
import com.agroruta.weather.application.ports.out.WeatherApiPort;
import com.agroruta.weather.domain.Pronostico;
import com.agroruta.weather.domain.Weather;
import com.agroruta.shared.exception.ExternalApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OpenWeatherAdapter implements WeatherApiPort {

    private final RestTemplate restTemplate;

    private final String apiKey;
    private final String baseUrl;

    public OpenWeatherAdapter(RestTemplate restTemplate, ClimaProperties propiedades) {
        this.restTemplate = restTemplate;
        this.apiKey = propiedades.key();
        this.baseUrl = propiedades.baseUrl();
    }

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

    /** Las próximas 8 franjas de 3 horas del pronóstico gratuito de OpenWeather = 24 horas */
    @Override
    @SuppressWarnings("unchecked")
    public Pronostico fetchPronostico24h(double lat, double lon) {
        String url = String.format(java.util.Locale.ROOT, "%s/forecast?lat=%s&lon=%s&cnt=8&appid=%s&units=metric",
                baseUrl, lat, lon, apiKey);
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> franjas = response == null ? null : (List<Map<String, Object>>) response.get("list");
            if (franjas == null || franjas.isEmpty()) throw new ExternalApiException("Pronóstico vacío de OpenWeatherMap");
            return resumir(franjas);
        } catch (ExternalApiException e) {
            throw e;
        } catch (RuntimeException e) {
            // Sin el mensaje original: trae la URL con la clave de la API, y este mensaje llega al navegador
            throw new ExternalApiException("Error al consultar el pronóstico de OpenWeatherMap", e);
        }
    }

    /** 16 franjas de 3 horas = 48 horas */
    @Override
    @SuppressWarnings("unchecked")
    public List<com.agroruta.weather.domain.FranjaClima> fetchFranjas48h(double lat, double lon) {
        String url = String.format(java.util.Locale.ROOT, "%s/forecast?lat=%s&lon=%s&cnt=16&appid=%s&units=metric",
                baseUrl, lat, lon, apiKey);
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> franjas = response == null ? null : (List<Map<String, Object>>) response.get("list");
            if (franjas == null || franjas.isEmpty()) throw new ExternalApiException("Pronóstico vacío de OpenWeatherMap");
            return franjas(franjas);
        } catch (ExternalApiException e) {
            throw e;
        } catch (RuntimeException e) {
            // Sin el mensaje original: trae la URL con la clave de la API
            throw new ExternalApiException("Error al consultar el pronóstico de OpenWeatherMap", e);
        }
    }

    private static final java.time.ZoneId ZONA = ZonaHoraria.COLOMBIA;

    @SuppressWarnings("unchecked")
    static List<com.agroruta.weather.domain.FranjaClima> franjas(List<Map<String, Object>> lista) {
        List<com.agroruta.weather.domain.FranjaClima> res = new java.util.ArrayList<>();
        for (Map<String, Object> f : lista) {
            if (!(f.get("dt") instanceof Number dt)) continue;
            Map<String, Object> main = (Map<String, Object>) f.get("main");
            Map<String, Object> rain = (Map<String, Object>) f.get("rain");
            Map<String, Object> wind = (Map<String, Object>) f.get("wind");
            res.add(new com.agroruta.weather.domain.FranjaClima(
                    java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(dt.longValue()), ZONA),
                    main == null ? 0 : numero(main.get("temp"), 0),
                    rain == null ? 0 : numero(rain.get("3h"), 0),
                    numero(f.get("pop"), 0),
                    wind == null ? 0 : numero(wind.get("speed"), 0)));
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    static Pronostico resumir(List<Map<String, Object>> franjas) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, lluvia = 0, viento = 0;
        for (Map<String, Object> f : franjas) {
            Map<String, Object> main = (Map<String, Object>) f.get("main");
            if (main != null) {
                min = Math.min(min, numero(main.get("temp_min"), numero(main.get("temp"), min)));
                max = Math.max(max, numero(main.get("temp_max"), numero(main.get("temp"), max)));
            }
            Map<String, Object> rain = (Map<String, Object>) f.get("rain");
            if (rain != null) lluvia += numero(rain.get("3h"), 0);
            Map<String, Object> wind = (Map<String, Object>) f.get("wind");
            if (wind != null) viento = Math.max(viento, numero(wind.get("speed"), 0));
        }
        return new Pronostico(min, max, lluvia, viento);
    }

    private static double numero(Object v, double siFalta) {
        return v instanceof Number n ? n.doubleValue() : siFalta;
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

            Weather weather = new Weather(
                    (String) response.get("name"),
                    (String) sys.get("country"),
                    ((Number) main.get("temp")).doubleValue(),
                    ((Number) main.get("feels_like")).doubleValue(),
                    ((Number) main.get("humidity")).intValue(),
                    ((Number) wind.get("speed")).doubleValue(),
                    (String) weatherInfo.get("description"),
                    (String) weatherInfo.get("icon"));

            return weather;
        } catch (ExternalApiException e) {
            throw e;
        } catch (RuntimeException e) {
            // Sin el mensaje original: trae la URL con la clave de la API, y este mensaje llega al navegador
            throw new ExternalApiException("Error al consultar OpenWeatherMap", e);
        }
    }
}