package com.agroruta.shared.util;

import com.agroruta.crop.domain.EstadoCultivo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EnumParser")
class EnumParserTest {

    @Test
    @DisplayName("acepta el valor sin importar mayúsculas ni espacios")
    void aceptaValorFlexible() {
        assertThat(EnumParser.parse(EstadoCultivo.class, " crecimiento ", "etapa"))
                .isEqualTo(EstadoCultivo.CRECIMIENTO);
    }

    @Test
    @DisplayName("rechaza un valor desconocido listando las opciones, sin nombres de clases")
    void rechazaValorDesconocido() {
        assertThatThrownBy(() -> EnumParser.parse(EstadoCultivo.class, "ROJA", "etapa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Valor no válido para etapa: 'ROJA'")
                .hasMessageContaining("GERMINACION, CRECIMIENTO, PRODUCCION, COSECHA, FINALIZADO")
                .hasMessageNotContaining("com.agroruta");
    }

    @Test
    @DisplayName("rechaza un valor vacío")
    void rechazaVacio() {
        assertThatThrownBy(() -> EnumParser.parse(EstadoCultivo.class, " ", "etapa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Falta el valor de etapa.");
    }
}
