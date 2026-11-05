package com.agroruta.weather.infrastructure;

import com.agroruta.weather.domain.Pronostico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenWeatherAdapter.resumir")
class OpenWeatherPronosticoTest {

    @Test
    @DisplayName("mínima, máxima, lluvia acumulada y viento más fuerte de las franjas")
    void resume() {
        List<Map<String, Object>> franjas = List.of(
                Map.of("main", Map.of("temp", 8.0, "temp_min", 6.5, "temp_max", 9.0), "wind", Map.of("speed", 3.1),
                        "rain", Map.of("3h", 4.2)),
                Map.of("main", Map.of("temp", 2.0, "temp_min", 1.8, "temp_max", 2.4), "wind", Map.of("speed", 6.0)),
                Map.of("main", Map.of("temp", 17.0, "temp_min", 16.0, "temp_max", 18.5), "rain", Map.of("3h", 1.3)));
        Pronostico p = OpenWeatherAdapter.resumir(franjas);
        assertThat(p.tempMin()).isEqualTo(1.8);
        assertThat(p.tempMax()).isEqualTo(18.5);
        assertThat(p.lluviaMm()).isEqualTo(5.5, org.assertj.core.data.Offset.offset(0.001));
        assertThat(p.vientoMax()).isEqualTo(6.0);
        assertThat(p.riesgoHelada()).isTrue();
        assertThat(p.lluviaFuerte()).isFalse();
        assertThat(p.lluviaLavaProducto()).isTrue();
    }
}
