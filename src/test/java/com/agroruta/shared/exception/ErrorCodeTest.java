package com.agroruta.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode")
class ErrorCodeTest {

    @Test
    @DisplayName("debe contener exactamente 8 códigos definidos")
    void shouldContainExactlyEightCodes() {
        assertThat(ErrorCode.values()).hasSize(8);
    }

    @ParameterizedTest(name = "{0} debe tener code y defaultMessage no nulos ni vacíos")
    @EnumSource(ErrorCode.class)
    @DisplayName("todos los valores deben tener code y defaultMessage válidos")
    void allValuesShouldHaveNonBlankCodeAndMessage(ErrorCode code) {
        assertThat(code.getCode()).isNotBlank();
        assertThat(code.getDefaultMessage()).isNotBlank();
    }

    @ParameterizedTest(name = "el code de {0} debe seguir el patrón AGR-NNN")
    @EnumSource(ErrorCode.class)
    @DisplayName("todos los codes deben cumplir el patrón AGR-NNN")
    void allCodesShouldMatchPattern(ErrorCode code) {
        assertThat(code.getCode()).matches("AGR-\\d{3}");
    }

    @Test
    @DisplayName("los codes deben ser únicos entre todos los valores del enum")
    void codesShouldBeUnique() {
        long distinctCodes = java.util.Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .distinct()
                .count();

        assertThat(distinctCodes).isEqualTo(ErrorCode.values().length);
    }

    // Verificaciones por valor concreto
    @Test
    @DisplayName("RESOURCE_NOT_FOUND debe tener code AGR-001")
    void resourceNotFoundShouldHaveCorrectCode() {
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.getCode()).isEqualTo("AGR-001");
    }

    @Test
    @DisplayName("RESOURCE_ALREADY_EXISTS debe tener code AGR-002")
    void resourceAlreadyExistsShouldHaveCorrectCode() {
        assertThat(ErrorCode.RESOURCE_ALREADY_EXISTS.getCode()).isEqualTo("AGR-002");
    }

    @Test
    @DisplayName("BUSINESS_RULE_VIOLATION debe tener code AGR-010")
    void businessRuleViolationShouldHaveCorrectCode() {
        assertThat(ErrorCode.BUSINESS_RULE_VIOLATION.getCode()).isEqualTo("AGR-010");
    }

    @Test
    @DisplayName("INVALID_OPERATION debe tener code AGR-011")
    void invalidOperationShouldHaveCorrectCode() {
        assertThat(ErrorCode.INVALID_OPERATION.getCode()).isEqualTo("AGR-011");
    }

    @Test
    @DisplayName("INVALID_ARGUMENT debe tener code AGR-020")
    void invalidArgumentShouldHaveCorrectCode() {
        assertThat(ErrorCode.INVALID_ARGUMENT.getCode()).isEqualTo("AGR-020");
    }

    @Test
    @DisplayName("VALIDATION_FAILED debe tener code AGR-021")
    void validationFailedShouldHaveCorrectCode() {
        assertThat(ErrorCode.VALIDATION_FAILED.getCode()).isEqualTo("AGR-021");
    }

    @Test
    @DisplayName("INTERNAL_ERROR debe tener code AGR-500")
    void internalErrorShouldHaveCorrectCode() {
        assertThat(ErrorCode.INTERNAL_ERROR.getCode()).isEqualTo("AGR-500");
    }

    @Test
    @DisplayName("EXTERNAL_SERVICE_ERROR debe tener code AGR-501")
    void externalServiceErrorShouldHaveCorrectCode() {
        assertThat(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode()).isEqualTo("AGR-501");
    }
}