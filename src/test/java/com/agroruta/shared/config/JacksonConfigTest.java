package com.agroruta.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JacksonConfig")
class JacksonConfigTest {

    private final JacksonConfig config = new JacksonConfig();

    @Test
    @DisplayName("objectMapper() debe retornar una instancia no nula")
    void objectMapperShouldNotBeNull() {
        assertThat(config.objectMapper()).isNotNull();
    }

    @Test
    @DisplayName("debe tener el módulo JavaTimeModule registrado")
    void shouldHaveJavaTimeModuleRegistered() {
        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper.getRegisteredModuleIds())
                .anyMatch(id -> id.toString().contains("jackson-datatype-jsr310"));
    }

    @Test
    @DisplayName("WRITE_DATES_AS_TIMESTAMPS debe estar deshabilitado")
    void shouldDisableWriteDatesAsTimestamps() {
        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }

    @Test
    @DisplayName("debe serializar LocalDateTime como String ISO-8601, no como array numérico")
    void shouldSerializeLocalDateTimeAsIsoString() throws Exception {
        ObjectMapper mapper = config.objectMapper();
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 0);

        String json = mapper.writeValueAsString(dateTime);

        assertThat(json)
                .startsWith("\"")
                .contains("2024-06-15")
                .contains("10:30:00");
    }

    @Test
    @DisplayName("debe deserializar un String ISO-8601 de vuelta a LocalDateTime")
    void shouldDeserializeIsoStringToLocalDateTime() throws Exception {
        ObjectMapper mapper = config.objectMapper();
        LocalDateTime expected = LocalDateTime.of(2024, 6, 15, 10, 30, 0);

        LocalDateTime result = mapper.readValue("\"2024-06-15T10:30:00\"", LocalDateTime.class);

        assertThat(result).isEqualTo(expected);
    }
}