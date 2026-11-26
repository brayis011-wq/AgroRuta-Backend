package com.agroruta.weather.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WeatherTest {

    @Test
    void deberiaCrearWeatherConValoresCorrectos() {
        Weather weather = new Weather("Bogotá", "CO", 18.5, 17.0, 80, 3.5, "Nublado", "04d");

        assertEquals("Bogotá", weather.city());
        assertEquals("CO", weather.country());
        assertEquals(18.5, weather.temperature());
        assertEquals(17.0, weather.feelsLike());
        assertEquals(80, weather.humidity());
        assertEquals(3.5, weather.windSpeed());
        assertEquals("Nublado", weather.description());
        assertEquals("04d", weather.icon());
    }

    @Test
    void dosWeatherConMismosValoresDeberianSerIguales() {
        Weather weather1 = new Weather("Bogotá", "CO", 18.5, 17.0, 80, 3.5, "Nublado", "04d");
        Weather weather2 = new Weather("Bogotá", "CO", 18.5, 17.0, 80, 3.5, "Nublado", "04d");

        assertEquals(weather1, weather2);
        assertEquals(weather1.hashCode(), weather2.hashCode());
    }

    @Test
    void conOtraTemperaturaNoEsElMismoClima() {
        Weather weather1 = new Weather("Bogotá", "CO", 18.5, 17.0, 80, 3.5, "Nublado", "04d");
        Weather weather2 = new Weather("Bogotá", "CO", 22.0, 17.0, 80, 3.5, "Nublado", "04d");

        assertNotEquals(weather1, weather2);
    }
}
