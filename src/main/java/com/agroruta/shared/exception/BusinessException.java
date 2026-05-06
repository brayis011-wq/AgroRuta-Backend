package com.agroruta.shared.exception;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    // ← Constructor de compatibilidad con el código existente
    public BusinessException(String message) {
        super(message);
        this.errorCode = ErrorCode.BUSINESS_RULE_VIOLATION;
    }

    // Constructor solo con ErrorCode (usa el mensaje por defecto del enum)
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    // Constructor con ErrorCode + detalle personalizado
    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}