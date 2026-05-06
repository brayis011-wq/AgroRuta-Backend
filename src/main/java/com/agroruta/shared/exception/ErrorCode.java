// shared/exception/ErrorCode.java
package com.agroruta.shared.exception;

public enum ErrorCode {

    // Recursos
    RESOURCE_NOT_FOUND("AGR-001", "Recurso no encontrado"),
    RESOURCE_ALREADY_EXISTS("AGR-002", "El recurso ya existe"),

    // Negocio
    BUSINESS_RULE_VIOLATION("AGR-010", "Violación de regla de negocio"),
    INVALID_OPERATION("AGR-011", "Operación no válida"),

    // Validación
    INVALID_ARGUMENT("AGR-020", "Argumento inválido"),
    VALIDATION_FAILED("AGR-021", "Error de validación"),

    // Sistema
    INTERNAL_ERROR("AGR-500", "Error interno del servidor"),
    EXTERNAL_SERVICE_ERROR("AGR-501", "Error en servicio externo");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}