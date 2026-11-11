// shared/exception/ErrorCode.java
package com.agroruta.shared.exception;

public enum ErrorCode {

    // Recursos
    RESOURCE_NOT_FOUND("AGR-001", "Recurso no encontrado"),
    RESOURCE_ALREADY_EXISTS("AGR-002", "El recurso ya existe"),

    // Negocio
    BUSINESS_RULE_VIOLATION("AGR-010", "Violación de regla de negocio"),
    INVALID_OPERATION("AGR-011", "Operación no válida"),
    /** Cosecha dentro del periodo de carencia de una fumigación: el frontend pide confirmación */
    HARVEST_IN_WITHDRAWAL("AGR-012", "Cosecha en periodo de carencia"),

    // Validación
    INVALID_ARGUMENT("AGR-020", "Argumento inválido"),
    VALIDATION_FAILED("AGR-021", "Error de validación"),

    // Seguridad
    ACCESS_DENIED("AGR-030", "Acceso denegado"),
    AUTHENTICATION_FAILED("AGR-031", "Credenciales inválidas"),
    TOO_MANY_ATTEMPTS("AGR-032", "Demasiados intentos"),
    EMAIL_NOT_VERIFIED("AGR-033", "Correo sin confirmar"),
    /** La contraseña es correcta, pero la cuenta pide el código de la app de autenticación */
    TWO_FACTOR_REQUIRED("AGR-034", "Falta el código de verificación"),
    TWO_FACTOR_INVALID("AGR-035", "Código de verificación incorrecto"),

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