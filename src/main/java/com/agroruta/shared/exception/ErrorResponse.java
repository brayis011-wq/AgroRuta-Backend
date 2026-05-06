// shared/exception/ErrorResponse.java
package com.agroruta.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String errorCode;
    private final int status;
    private final String message;
    private final LocalDateTime timestamp;
    private final String traceId;           // para correlacionar con los logs
    private Map<String, String> fieldErrors; // para errores de validación

    public ErrorResponse(int status, String errorCode, String message, String traceId) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.traceId = traceId;
    }

    // Builder estático para uso limpio
    public static ErrorResponse of(int status, ErrorCode errorCode, String message, String traceId) {
        return new ErrorResponse(status, errorCode.getCode(), message, traceId);
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    // Getters
    public String getErrorCode() { return errorCode; }
    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getTraceId() { return traceId; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}