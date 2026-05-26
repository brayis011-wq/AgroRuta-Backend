package com.agroruta.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig")
class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    @DisplayName("customOpenAPI() debe retornar una instancia no nula")
    void openApiShouldNotBeNull() {
        assertThat(config.customOpenAPI()).isNotNull();
    }

    @Nested
    @DisplayName("Esquema de seguridad bearer-key")
    class BearerKeyScheme {

        @Test
        @DisplayName("debe registrar un esquema llamado 'bearer-key'")
        void shouldRegisterBearerKeyScheme() {
            OpenAPI api = config.customOpenAPI();

            assertThat(api.getComponents().getSecuritySchemes())
                    .containsKey("bearer-key");
        }

        @Test
        @DisplayName("el tipo del esquema debe ser HTTP")
        void shouldBeHttpType() {
            SecurityScheme scheme = getScheme();

            assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        }

        @Test
        @DisplayName("el scheme HTTP debe ser 'bearer'")
        void shouldUseBearerScheme() {
            SecurityScheme scheme = getScheme();

            assertThat(scheme.getScheme()).isEqualTo("bearer");
        }

        @Test
        @DisplayName("el bearerFormat debe ser 'JWT'")
        void shouldHaveJwtBearerFormat() {
            SecurityScheme scheme = getScheme();

            assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
        }

        private SecurityScheme getScheme() {
            return config.customOpenAPI()
                    .getComponents()
                    .getSecuritySchemes()
                    .get("bearer-key");
        }
    }

    @Nested
    @DisplayName("Requisito de seguridad global")
    class GlobalSecurityRequirement {

        @Test
        @DisplayName("debe existir al menos un SecurityRequirement global")
        void shouldHaveAtLeastOneGlobalSecurityRequirement() {
            List<SecurityRequirement> requirements = config.customOpenAPI().getSecurity();

            assertThat(requirements).isNotEmpty();
        }

        @Test
        @DisplayName("el SecurityRequirement global debe incluir 'bearer-key'")
        void shouldIncludeBearerKeyInGlobalRequirement() {
            List<SecurityRequirement> requirements = config.customOpenAPI().getSecurity();

            boolean hasBearerKey = requirements.stream()
                    .anyMatch(req -> req.containsKey("bearer-key"));

            assertThat(hasBearerKey).isTrue();
        }
    }
}