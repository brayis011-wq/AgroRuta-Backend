// com/agroruta/weather/application/WeatherService.java
package com.agroruta.weather.application;

import com.agroruta.weather.application.ports.in.WeatherUseCase;
import com.agroruta.weather.application.ports.out.WeatherApiPort;
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

    @Override
    public com.agroruta.weather.domain.Pronostico getPronostico24h(double lat, double lon) {
        return weatherApiPort.fetchPronostico24h(lat, lon);
    }

    private record Guardado(long cuando, java.util.List<com.agroruta.weather.domain.FranjaClima> franjas) {}

    private static final long VIGENCIA_MS = 30 * 60 * 1000L;
    private static final int MAX_GUARDADOS = 500;
    private final java.util.Map<String, Guardado> franjasGuardadas = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public java.util.List<com.agroruta.weather.domain.FranjaClima> getFranjas48h(double lat, double lon) {
        // Dos decimales ≈ 1 km: fincas vecinas comparten el mismo pronóstico
        String clave = String.format(java.util.Locale.ROOT, "%.2f,%.2f", lat, lon);
        long ahora = System.currentTimeMillis();
        Guardado g = franjasGuardadas.get(clave);
        if (g != null && ahora - g.cuando() < VIGENCIA_MS) return g.franjas();
        java.util.List<com.agroruta.weather.domain.FranjaClima> franjas = weatherApiPort.fetchFranjas48h(lat, lon);
        if (franjasGuardadas.size() >= MAX_GUARDADOS) franjasGuardadas.clear();
        franjasGuardadas.put(clave, new Guardado(ahora, franjas));
        return franjas;
    }
}