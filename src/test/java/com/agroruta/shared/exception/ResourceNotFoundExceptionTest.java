package com.agroruta.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResourceNotFoundException")
class ResourceNotFoundExceptionTest {

    @Nested
    @DisplayName("Constructor con String")
    class StringConstructor {

        @Test
        @DisplayName("debe usar el mensaje recibido como message de la excepción")
        void shouldSetMessageFromString() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Lote no encontrado");
            assertThat(ex.getMessage()).isEqualTo("Lote no encontrado");
        }

        @Test
        @DisplayName("debe asignar RESOURCE_NOT_FOUND como errorCode por defecto")
        void shouldDefaultToResourceNotFound() {
            ResourceNotFoundException ex = new ResourceNotFoundException("cualquier mensaje");
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("debe extender RuntimeException")
        void shouldBeARuntimeException() {
            assertThat(new ResourceNotFoundException("msg")).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Constructor semántico con recurso e id")
    class ResourceAndIdConstructor {

        @Test
        @DisplayName("debe generar el mensaje con formato esperado para id numérico")
        void shouldFormatMessageWithNumericId() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Agricultor", 42L);
            assertThat(ex.getMessage()).isEqualTo("Agricultor con id '42' no fue encontrado");
        }

        @Test
        @DisplayName("debe generar el mensaje con formato esperado para id de tipo String")
        void shouldFormatMessageWithStringId() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Predio", "PRDX-001");
            assertThat(ex.getMessage()).isEqualTo("Predio con id 'PRDX-001' no fue encontrado");
        }

        @Test
        @DisplayName("debe asignar RESOURCE_NOT_FOUND como errorCode")
        void shouldUseResourceNotFoundCode() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Cultivo", 7);
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("el id null debe representarse en el mensaje como 'null'")
        void shouldHandleNullId() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Entidad", null);
            assertThat(ex.getMessage()).isEqualTo("Entidad con id 'null' no fue encontrado");
        }
    }

    @Nested
    @DisplayName("Constructor con ErrorCode personalizado")
    class CustomErrorCodeConstructor {

        @Test
        @DisplayName("debe usar el detalle personalizado como message")
        void shouldUseCustomDetailAsMessage() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS, "El recurso con ese código ya fue registrado");

            assertThat(ex.getMessage()).isEqualTo("El recurso con ese código ya fue registrado");
        }

        @Test
        @DisplayName("debe conservar el ErrorCode recibido")
        void shouldPreserveCustomErrorCode() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS, "detalle");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("debe funcionar con cualquier valor del enum")
        void shouldWorkWithAnyErrorCode() {
            for (ErrorCode code : ErrorCode.values()) {
                ResourceNotFoundException ex = new ResourceNotFoundException(code, "detalle");
                assertThat(ex.getErrorCode()).isEqualTo(code);
            }
        }
    }
}