package com.agroruta.weather.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherTest {

    @Test
    void deberiaCrearWeatherConValoresCorrectos() {
        Weather weather = new Weather("Bogotá", "CO", 18.5, 17.0, 80, 3.5, "Nublado", "04d");

        assertEquals("Bogotá", weather.getCity());
        assertEquals("CO", weather.getCountry());
        assertEquals(18.5, weather.getTemperature());
        assertEquals(17.0, weather.getFeelsLike());
        assertEquals(80, weather.getHumidity());
        assertEquals(3.5, weather.getWindSpeed());
        assertEquals("Nublado", weather.getDescription());
        assertEquals("04d", weather.getIcon());
    }

    @Test
    void deberiaCrearWeatherVacioConNoArgsConstructor() {
        Weather weather = new Weather();

        assertNull(weather.getCity());
        assertNull(weather.getCountry());
        assertEquals(0.0, weather.getTemperature());
        assertEquals(0.0, weather.getFeelsLike());
        assertEquals(0, weather.getHumidity());
        assertEquals(0.0, weather.getWindSpeed());
        assertNull(weather.getDescription());
        assertNull(weather.getIcon());
    }

    @Test
    void deberiaPermitirCambiarTemperatura() {
        Weather weather = new Weather("Bogotá", "CO", 18.5, 17.0, 80, 3.5, "Nublado", "04d");

        weather.setTemperature(22.0);

        assertEquals(22.0, weather.getTemperature());
    }

    @Test
    void deberiaPermitirCambiarDescripcion() {
        Weather weather = new Weather("Bogotá", "CO", 18.5, 17.0, 80, 3.5, "Nublado", "04d");

        weather.setDescription("Soleado");

        assertEquals("Soleado", weather.getDescription());
    }

    @Test
    void dosWeatherConMismosValoresDeberianSerIguales() {
        Weather weather1 = new Weather("Bogotá", "CO", 18.5, 17.0, 80, 3.5, "Nublado", "04d");
        Weather weather2 = new Weather("Bogotá", "CO", 18.5, 17.0, 80, 3.5, "Nublado", "04d");

        assertEquals(weather1, weather2);
    }
}