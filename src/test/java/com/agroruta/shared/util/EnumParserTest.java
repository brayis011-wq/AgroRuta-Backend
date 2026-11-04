package com.agroruta.shared.util;

import com.agroruta.crop.domain.VariedadUchuva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EnumParser")
class EnumParserTest {

    @Test
    @DisplayName("acepta el valor sin importar mayúsculas ni espacios")
    void aceptaValorFlexible() {
        assertThat(EnumParser.parse(VariedadUchuva.class, " kenya ", "variedad"))
                .isEqualTo(VariedadUchuva.KENYA);
    }

    @Test
    @DisplayName("rechaza un valor desconocido listando las opciones, sin nombres de clases")
    void rechazaValorDesconocido() {
        assertThatThrownBy(() -> EnumParser.parse(VariedadUchuva.class, "ROJA", "variedad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Valor no válido para variedad: 'ROJA'")
                .hasMessageContaining("COLOMBIA, GIGANTE, KENYA")
                .hasMessageNotContaining("com.agroruta");
    }

    @Test
    @DisplayName("rechaza un valor vacío")
    void rechazaVacio() {
        assertThatThrownBy(() -> EnumParser.parse(VariedadUchuva.class, " ", "variedad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Falta el valor de variedad.");
    }
}
