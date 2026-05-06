package com.agroruta.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;

    // ← Constructor que ya tenías antes (compatibilidad)
    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = ErrorCode.RESOURCE_NOT_FOUND;
    }

    // Constructor semántico: recurso + id
    public ResourceNotFoundException(String resource, Object id) {
        super(String.format("%s con id '%s' no fue encontrado", resource, id));
        this.errorCode = ErrorCode.RESOURCE_NOT_FOUND;
    }

    // Constructor con ErrorCode personalizado
    public ResourceNotFoundException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}