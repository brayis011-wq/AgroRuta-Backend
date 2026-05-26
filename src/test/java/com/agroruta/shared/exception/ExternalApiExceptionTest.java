package com.agroruta.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExternalApiException")
class ExternalApiExceptionTest {

    @Nested
    @DisplayName("Constructor con mensaje")
    class MessageConstructor {

        @Test
        @DisplayName("debe asignar el mensaje correctamente")
        void shouldSetMessage() {
            ExternalApiException ex = new ExternalApiException("Timeout al conectar con SIIGA");
            assertThat(ex.getMessage()).isEqualTo("Timeout al conectar con SIIGA");
        }

        @Test
        @DisplayName("debe extender RuntimeException")
        void shouldBeARuntimeException() {
            assertThat(new ExternalApiException("msg")).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("cause debe ser null cuando no se provee")
        void shouldHaveNullCauseWhenNotProvided() {
            ExternalApiException ex = new ExternalApiException("msg");
            assertThat(ex.getCause()).isNull();
        }
    }

    @Nested
    @DisplayName("Constructor con mensaje y causa")
    class MessageAndCauseConstructor {

        @Test
        @DisplayName("debe asignar el mensaje correctamente")
        void shouldSetMessage() {
            Throwable cause = new RuntimeException("Connection refused");
            ExternalApiException ex = new ExternalApiException("Error en AGROSAVIA", cause);

            assertThat(ex.getMessage()).isEqualTo("Error en AGROSAVIA");
        }

        @Test
        @DisplayName("debe asignar la causa correctamente")
        void shouldSetCause() {
            Throwable cause = new RuntimeException("Connection refused");
            ExternalApiException ex = new ExternalApiException("Error externo", cause);

            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("el mensaje de la causa debe ser accesible")
        void shouldPreserveCauseMessage() {
            Throwable cause = new IllegalStateException("socket closed");
            ExternalApiException ex = new ExternalApiException("Fallo en API externa", cause);

            assertThat(ex.getCause().getMessage()).isEqualTo("socket closed");
        }

        @Test
        @DisplayName("debe permitir encadenar múltiples causas")
        void shouldAllowChainedCauses() {
            Throwable root = new RuntimeException("root cause");
            Throwable intermediate = new RuntimeException("intermediate", root);
            ExternalApiException ex = new ExternalApiException("top level", intermediate);

            assertThat(ex.getCause()).isSameAs(intermediate);
            assertThat(ex.getCause().getCause()).isSameAs(root);
        }
    }
}