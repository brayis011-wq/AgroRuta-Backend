// com/agroruta/weather/application/WeatherService.java
package com.agroruta.weather.application;

import com.agroruta.weather.application.ports.in.WeatherUseCase;
import com.agroruta.weather.application.ports.out.WeatherApiPort;
import com.agroruta.weather.domain.FranjaClima;
import com.agroruta.weather.domain.Pronostico;
import com.agroruta.weather.domain.Weather;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * El clima con copias de corta duración: el actual cambia poco en diez minutos y los pronósticos en media hora.
 * Así el inicio, el menú lateral y el detalle de la siembra no gastan una llamada a OpenWeather cada uno.
 */
@Service
public class WeatherService implements WeatherUseCase {

    static final Duration VIGENCIA_ACTUAL = Duration.ofMinutes(10);
    static final Duration VIGENCIA_PRONOSTICO = Duration.ofMinutes(30);
    private static final int MAX_GUARDADOS = 500;

    private final WeatherApiPort weatherApiPort;
    private final CopiaBreve<Weather> actualPorCiudad;
    private final CopiaBreve<Weather> actualPorLugar;
    private final CopiaBreve<Pronostico> pronosticos;
    private final CopiaBreve<List<FranjaClima>> franjas;

    public WeatherService(WeatherApiPort weatherApiPort, Clock reloj) {
        this.weatherApiPort = weatherApiPort;
        if (reloj == null) reloj = Clock.systemUTC(); // las pruebas con @InjectMocks no traen reloj
        this.actualPorCiudad = new CopiaBreve<>(VIGENCIA_ACTUAL, MAX_GUARDADOS, reloj);
        this.actualPorLugar = new CopiaBreve<>(VIGENCIA_ACTUAL, MAX_GUARDADOS, reloj);
        this.pronosticos = new CopiaBreve<>(VIGENCIA_PRONOSTICO, MAX_GUARDADOS, reloj);
        this.franjas = new CopiaBreve<>(VIGENCIA_PRONOSTICO, MAX_GUARDADOS, reloj);
    }

    @Override
    public Weather getWeatherByCity(String city) {
        return actualPorCiudad.obtener(CopiaBreve.ciudad(city), () -> weatherApiPort.fetchWeatherByCity(city));
    }

    @Override
    public Weather getWeatherByCoordinates(double lat, double lon) {
        return actualPorLugar.obtener(
                CopiaBreve.lugar(lat, lon), () -> weatherApiPort.fetchWeatherByCoordinates(lat, lon));
    }

    @Override
    public Pronostico getPronostico24h(double lat, double lon) {
        return pronosticos.obtener(CopiaBreve.lugar(lat, lon), () -> weatherApiPort.fetchPronostico24h(lat, lon));
    }

    @Override
    public List<FranjaClima> getFranjas48h(double lat, double lon) {
        return franjas.obtener(CopiaBreve.lugar(lat, lon), () -> weatherApiPort.fetchFranjas48h(lat, lon));
    }
}
