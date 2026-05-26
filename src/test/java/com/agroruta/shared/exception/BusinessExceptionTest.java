package com.agroruta.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessException")
class BusinessExceptionTest {

    @Nested
    @DisplayName("Constructor con String")
    class StringConstructor {

        @Test
        @DisplayName("debe usar el mensaje recibido como message de la excepción")
        void shouldSetMessageFromString() {
            BusinessException ex = new BusinessException("Precio negativo no permitido");

            assertThat(ex.getMessage()).isEqualTo("Precio negativo no permitido");
        }

        @Test
        @DisplayName("debe asignar BUSINESS_RULE_VIOLATION como errorCode por defecto")
        void shouldDefaultToBusinessRuleViolation() {
            BusinessException ex = new BusinessException("cualquier mensaje");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        @Test
        @DisplayName("debe extender RuntimeException")
        void shouldBeARuntimeException() {
            assertThat(new BusinessException("msg")).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Constructor con ErrorCode")
    class ErrorCodeConstructor {

        @Test
        @DisplayName("debe usar el defaultMessage del enum como message")
        void shouldUseDefaultMessageFromEnum() {
            BusinessException ex = new BusinessException(ErrorCode.INVALID_OPERATION);

            assertThat(ex.getMessage()).isEqualTo(ErrorCode.INVALID_OPERATION.getDefaultMessage());
        }

        @Test
        @DisplayName("debe conservar el ErrorCode recibido")
        void shouldPreserveErrorCode() {
            BusinessException ex = new BusinessException(ErrorCode.INVALID_OPERATION);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPERATION);
        }

        @Test
        @DisplayName("debe funcionar con cualquier valor del enum")
        void shouldWorkWithAnyErrorCode() {
            for (ErrorCode code : ErrorCode.values()) {
                BusinessException ex = new BusinessException(code);
                assertThat(ex.getErrorCode()).isEqualTo(code);
                assertThat(ex.getMessage()).isEqualTo(code.getDefaultMessage());
            }
        }
    }

    @Nested
    @DisplayName("Constructor con ErrorCode y detalle")
    class ErrorCodeAndDetailConstructor {

        @Test
        @DisplayName("debe usar el detalle personalizado como message")
        void shouldUseCustomDetailAsMessage() {
            BusinessException ex = new BusinessException(ErrorCode.VALIDATION_FAILED, "El campo RUT es inválido");

            assertThat(ex.getMessage()).isEqualTo("El campo RUT es inválido");
        }

        @Test
        @DisplayName("debe conservar el ErrorCode recibido")
        void shouldPreserveErrorCode() {
            BusinessException ex = new BusinessException(ErrorCode.VALIDATION_FAILED, "detalle");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("el detalle personalizado debe tener prioridad sobre el defaultMessage del enum")
        void customDetailShouldOverrideDefaultMessage() {
            String customDetail = "detalle específico";
            BusinessException ex = new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, customDetail);

            assertThat(ex.getMessage())
                    .isEqualTo(customDetail)
                    .isNotEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION.getDefaultMessage());
        }
    }
}