package com.agroruta.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.time.temporal.ChronoUnit;

@DisplayName("ErrorResponse")
class ErrorResponseTest {

    private static final String TRACE_ID = "trace-abc-123";

    @Nested
    @DisplayName("Constructor directo")
    class DirectConstructor {

        @Test
        @DisplayName("debe asignar status correctamente")
        void shouldSetStatus() {
            ErrorResponse response = new ErrorResponse(404, "AGR-001", "No encontrado", TRACE_ID);
            assertThat(response.getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("debe asignar errorCode correctamente")
        void shouldSetErrorCode() {
            ErrorResponse response = new ErrorResponse(404, "AGR-001", "No encontrado", TRACE_ID);
            assertThat(response.getErrorCode()).isEqualTo("AGR-001");
        }

        @Test
        @DisplayName("debe asignar message correctamente")
        void shouldSetMessage() {
            ErrorResponse response = new ErrorResponse(404, "AGR-001", "No encontrado", TRACE_ID);
            assertThat(response.getMessage()).isEqualTo("No encontrado");
        }

        @Test
        @DisplayName("debe asignar traceId correctamente")
        void shouldSetTraceId() {
            ErrorResponse response = new ErrorResponse(404, "AGR-001", "No encontrado", TRACE_ID);
            assertThat(response.getTraceId()).isEqualTo(TRACE_ID);
        }

        @Test
        @DisplayName("timestamp debe inicializarse cercano al momento de creación")
        void timestampShouldBeNearNow() {
            LocalDateTime before = LocalDateTime.now();
            ErrorResponse response = new ErrorResponse(500, "AGR-500", "Error", TRACE_ID);
            LocalDateTime after = LocalDateTime.now();

            assertThat(response.getTimestamp())
                    .isAfterOrEqualTo(before.truncatedTo(ChronoUnit.SECONDS))
                    .isBeforeOrEqualTo(after.plusSeconds(1));
        }

        @Test
        @DisplayName("fieldErrors debe ser null si no fue asignado")
        void fieldErrorsShouldBeNullByDefault() {
            ErrorResponse response = new ErrorResponse(400, "AGR-020", "Error", TRACE_ID);
            assertThat(response.getFieldErrors()).isNull();
        }
    }

    @Nested
    @DisplayName("Factory method of()")
    class OfMethod {

        @Test
        @DisplayName("debe asignar el code del ErrorCode como errorCode")
        void shouldUseErrorCodeCode() {
            ErrorResponse response = ErrorResponse.of(404, ErrorCode.RESOURCE_NOT_FOUND, "No encontrado", TRACE_ID);
            assertThat(response.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("debe asignar el message personalizado recibido")
        void shouldSetCustomMessage() {
            String message = "El agricultor con id '5' no fue encontrado";
            ErrorResponse response = ErrorResponse.of(404, ErrorCode.RESOURCE_NOT_FOUND, message, TRACE_ID);
            assertThat(response.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("debe asignar el status recibido")
        void shouldSetStatus() {
            ErrorResponse response = ErrorResponse.of(422, ErrorCode.VALIDATION_FAILED, "Error de validación", TRACE_ID);
            assertThat(response.getStatus()).isEqualTo(422);
        }

        @Test
        @DisplayName("debe asignar el traceId recibido")
        void shouldSetTraceId() {
            ErrorResponse response = ErrorResponse.of(500, ErrorCode.INTERNAL_ERROR, "Error", "my-trace-id");
            assertThat(response.getTraceId()).isEqualTo("my-trace-id");
        }

        @Test
        @DisplayName("debe funcionar con cualquier valor de ErrorCode")
        void shouldWorkWithAnyErrorCode() {
            for (ErrorCode code : ErrorCode.values()) {
                ErrorResponse response = ErrorResponse.of(400, code, "msg", TRACE_ID);
                assertThat(response.getErrorCode()).isEqualTo(code.getCode());
            }
        }
    }

    @Nested
    @DisplayName("setFieldErrors()")
    class FieldErrors {

        @Test
        @DisplayName("debe almacenar los errores de campo correctamente")
        void shouldStoreFieldErrors() {
            ErrorResponse response = new ErrorResponse(400, "AGR-021", "Validación fallida", TRACE_ID);
            Map<String, String> errors = Map.of("nombre", "No puede estar vacío", "rut", "Formato inválido");

            response.setFieldErrors(errors);

            assertThat(response.getFieldErrors())
                    .containsEntry("nombre", "No puede estar vacío")
                    .containsEntry("rut", "Formato inválido");
        }

        @Test
        @DisplayName("debe permitir sobreescribir fieldErrors")
        void shouldAllowOverwritingFieldErrors() {
            ErrorResponse response = new ErrorResponse(400, "AGR-021", "Error", TRACE_ID);
            response.setFieldErrors(Map.of("campo1", "error1"));
            response.setFieldErrors(Map.of("campo2", "error2"));

            assertThat(response.getFieldErrors())
                    .containsOnlyKeys("campo2");
        }

        @Test
        @DisplayName("debe permitir asignar un mapa vacío")
        void shouldAcceptEmptyMap() {
            ErrorResponse response = new ErrorResponse(400, "AGR-021", "Error", TRACE_ID);
            response.setFieldErrors(Map.of());

            assertThat(response.getFieldErrors()).isEmpty();
        }
    }
}