
package com.agroruta.shared.exception;

import com.agroruta.shared.logging.MdcKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Duplicado en base de datos (unique constraint) ───────────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DataIntegrityViolationException ex, HttpServletRequest req) {

        log.warn("[{}] Duplicado detectado en {}: {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getRequestURI(),
                ex.getMostSpecificCause().getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                ErrorCode.RESOURCE_ALREADY_EXISTS,
                "Ya existe un registro con esos datos. Verifique e intente de nuevo.",
                MDC.get(MdcKeys.TRACE_ID)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ── Recurso no encontrado ────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {

        log.warn("[{}] Recurso no encontrado en {}: {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getRequestURI(),
                ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                MDC.get(MdcKeys.TRACE_ID)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ── Regla de negocio violada ─────────────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest req) {

        log.warn("[{}] Regla de negocio violada en {}: {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getRequestURI(),
                ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),   // 422 — correcto para reglas de negocio
                ex.getErrorCode(),
                ex.getMessage(),
                MDC.get(MdcKeys.TRACE_ID)
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    // ── Validación de Bean Validation (@Valid) ───────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.merge(fe.getField(), fe.getDefaultMessage(),
                    (existing, newMsg) -> existing); // si hay 2 errores en un campo, queda el primero
        }

        log.warn("[{}] Validación fallida en {}: {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getRequestURI(),
                fieldErrors);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_FAILED,
                "Error de validación en los datos enviados.",
                MDC.get(MdcKeys.TRACE_ID)
        );
        error.setFieldErrors(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ── Argumento inválido ───────────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {

        log.warn("[{}] Argumento inválido en {}: {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getRequestURI(),
                ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_ARGUMENT,
                ex.getMessage(),
                MDC.get(MdcKeys.TRACE_ID)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ── Servicio externo falló ───────────────────────────────────────────────
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponse> handleExternal(
            ExternalApiException ex, HttpServletRequest req) {

        log.error("[{}] Error en servicio externo en {}: {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getRequestURI(),
                ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_GATEWAY.value(),
                ErrorCode.EXTERNAL_SERVICE_ERROR,
                ex.getMessage(),
                MDC.get(MdcKeys.TRACE_ID)
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    // ── Error genérico — catch-all con stacktrace completo ───────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest req) {

        log.error("[{}] Error inesperado en {} {}: {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getMethod(),
                req.getRequestURI(),
                ex.getMessage(), ex);   // Logback imprime el stack trace automáticamente

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_ERROR,
                "Error interno del servidor. Contacte al administrador con el código: "
                        + MDC.get(MdcKeys.TRACE_ID),
                MDC.get(MdcKeys.TRACE_ID)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}