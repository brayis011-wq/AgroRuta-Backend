package com.agroruta.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorsConfig")
class CorsConfigTest {

    private final CorsConfig config = new CorsConfig();

    @Test
    @DisplayName("corsFilter() debe retornar una instancia no nula")
    void corsFilterShouldNotBeNull() {
        assertThat(config.corsFilter()).isNotNull();
    }

    @Nested
    @DisplayName("Configuración CORS")
    class CorsSettings {

        private CorsConfiguration getCorsConfiguration() throws Exception {
            CorsFilter filter = config.corsFilter();
            Field field = CorsFilter.class.getDeclaredField("configSource");
            field.setAccessible(true);
            UrlBasedCorsConfigurationSource source =
                    (UrlBasedCorsConfigurationSource) field.get(filter);

            Map<String, CorsConfiguration> configs = source.getCorsConfigurations();
            return configs.get("/**");
        }

        @Test
        @DisplayName("debe registrar la configuración para el patrón '/**'")
        void shouldRegisterGlobalPattern() throws Exception {
            CorsFilter filter = config.corsFilter();
            Field field = CorsFilter.class.getDeclaredField("configSource");
            field.setAccessible(true);
            UrlBasedCorsConfigurationSource source =
                    (UrlBasedCorsConfigurationSource) field.get(filter);

            assertThat(source.getCorsConfigurations()).containsKey("/**");
        }

        @Test
        @DisplayName("debe permitir el origen de Angular en dev (localhost:4200)")
        void shouldAllowAngularDevOrigin() throws Exception {
            CorsConfiguration cors = getCorsConfiguration();
            assertThat(cors.getAllowedOrigins()).contains("http://localhost:4200");
        }

        @Test
        @DisplayName("debe permitir el origen de Angular en Docker (localhost)")
        void shouldAllowAngularDockerOrigin() throws Exception {
            CorsConfiguration cors = getCorsConfiguration();
            assertThat(cors.getAllowedOrigins()).contains("http://localhost");
        }

        @Test
        @DisplayName("debe permitir el origen de Angular en Docker puerto 80 explícito")
        void shouldAllowAngularDockerPort80Origin() throws Exception {
            CorsConfiguration cors = getCorsConfiguration();
            assertThat(cors.getAllowedOrigins()).contains("http://localhost:80");
        }

        @Test
        @DisplayName("debe tener exactamente 3 orígenes permitidos")
        void shouldHaveExactlyThreeAllowedOrigins() throws Exception {
            CorsConfiguration cors = getCorsConfiguration();
            assertThat(cors.getAllowedOrigins()).hasSize(3);
        }

        @Test
        @DisplayName("debe permitir todos los métodos HTTP (*)")
        void shouldAllowAllMethods() throws Exception {
            CorsConfiguration cors = getCorsConfiguration();
            assertThat(cors.getAllowedMethods()).contains("*");
        }

        @Test
        @DisplayName("debe permitir todos los headers (*)")
        void shouldAllowAllHeaders() throws Exception {
            CorsConfiguration cors = getCorsConfiguration();
            assertThat(cors.getAllowedHeaders()).contains("*");
        }

        @Test
        @DisplayName("debe habilitar credenciales (cookies/Authorization)")
        void shouldAllowCredentials() throws Exception {
            CorsConfiguration cors = getCorsConfiguration();
            assertThat(cors.getAllowCredentials()).isTrue();
        }
    }
}