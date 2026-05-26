package com.agroruta.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppConfig")
class AppConfigTest {

    private final AppConfig config = new AppConfig();

    @Test
    @DisplayName("restTemplate() debe retornar una instancia no nula")
    void restTemplateShouldNotBeNull() {
        RestTemplate result = config.restTemplate();

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("cada invocación debe retornar una instancia distinta (prototype)")
    void restTemplateShouldReturnNewInstanceEachTime() {
        RestTemplate first = config.restTemplate();
        RestTemplate second = config.restTemplate();

        assertThat(first).isNotSameAs(second);
    }
}