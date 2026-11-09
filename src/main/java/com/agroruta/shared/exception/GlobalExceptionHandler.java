package com.agroruta.shared.exception;

import com.agroruta.shared.logging.MdcKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
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

        // Un duplicado es un conflicto (409); el resto de reglas de negocio, 422.
        // Demasiados intentos de inicio de sesión: 429.
        HttpStatus status = switch (ex.getErrorCode()) {
            case RESOURCE_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case TOO_MANY_ATTEMPTS -> HttpStatus.TOO_MANY_REQUESTS;
            case EMAIL_NOT_VERIFIED -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };

        ErrorResponse error = ErrorResponse.of(
                status.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                MDC.get(MdcKeys.TRACE_ID)
        );
        return ResponseEntity.status(status).body(error);
    }

    // ── Validación de Bean Validation (@Valid) ───────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.merge(fe.getField(), fe.getDefaultMessage(),
                    (existing, newMsg) -> existing); // si hay 2 errores en un campo, queda el primero
        }

        log.warn("[{}] Validación fallida en {}: {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getRequestURI(),
                fieldErrors);

        // El primer error va en "message" para que el frontend lo pueda mostrar tal cual;
        // la lista completa queda en fieldErrors.
        String mensaje = fieldErrors.isEmpty()
                ? "Error de validación en los datos enviados."
                : fieldErrors.values().iterator().next();

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_FAILED,
                mensaje,
                MDC.get(MdcKeys.TRACE_ID)
        );
        error.setFieldErrors(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ── Cuerpo ilegible: JSON mal formado, fecha inválida o valor de enum desconocido ──
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest req) {

        log.warn("[{}] Cuerpo de la petición ilegible en {}: {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getRequestURI(),
                ex.getMostSpecificCause().getMessage());

        // No se expone el detalle del parser al cliente.
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_FAILED,
                "El cuerpo de la petición no es válido. Revisa el formato de fechas, números y opciones.",
                MDC.get(MdcKeys.TRACE_ID)
        );
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

    // ── Estado inválido para la operación (p. ej. aprobar una nómina ya pagada) ──
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest req) {

        // Solo las de nuestro código son reglas de estado; las de librerías son fallos internos.
        if (!lanzadaPorAgroRuta(ex)) {
            return handleGeneral(ex, req);
        }

        log.warn("[{}] Operación no permitida en el estado actual en {}: {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getRequestURI(),
                ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                ErrorCode.INVALID_OPERATION,
                ex.getMessage(),
                MDC.get(MdcKeys.TRACE_ID)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ── Parámetro con tipo inválido (p. ej. /api/farms/abc) ─────────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {

        log.warn("[{}] Parámetro '{}' inválido en {}: {}",
                MDC.get(MdcKeys.TRACE_ID), ex.getName(), req.getRequestURI(), ex.getValue());

        return simple(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ARGUMENT,
                "El parámetro '" + ex.getName() + "' no tiene un valor válido.");
    }

    // ── Falta un parámetro obligatorio en la URL ─────────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest req) {

        log.warn("[{}] Falta el parámetro '{}' en {}",
                MDC.get(MdcKeys.TRACE_ID), ex.getParameterName(), req.getRequestURI());

        return simple(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ARGUMENT,
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'.");
    }

    // ── Ruta inexistente ─────────────────────────────────────────────────────
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest req) {

        log.warn("[{}] Ruta no encontrada: {} {}",
                MDC.get(MdcKeys.TRACE_ID), req.getMethod(), req.getRequestURI());

        return simple(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                "La ruta solicitada no existe.");
    }

    // ── Método HTTP no soportado (p. ej. PUT donde solo hay PATCH) ───────────
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {

        log.warn("[{}] Método {} no soportado en {}",
                MDC.get(MdcKeys.TRACE_ID), ex.getMethod(), req.getRequestURI());

        return simple(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.INVALID_OPERATION,
                "El método " + ex.getMethod() + " no está permitido en esta ruta.");
    }

    // ── Content-Type no soportado ────────────────────────────────────────────
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {

        log.warn("[{}] Content-Type no soportado en {}: {}",
                MDC.get(MdcKeys.TRACE_ID), req.getRequestURI(), ex.getContentType());

        return simple(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.INVALID_ARGUMENT,
                "Tipo de contenido no soportado. Envía application/json.");
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

    // ── 403: autenticado pero sin permiso sobre el recurso ───────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {

        log.warn("[{}] Acceso denegado en {} {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getMethod(),
                req.getRequestURI());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.ACCESS_DENIED,
                "No tienes permiso para acceder a este recurso",
                MDC.get(MdcKeys.TRACE_ID)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ── 401: cuenta desactivada por un administrador ─────────────────────────
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest req) {

        log.warn("[{}] Intento de login con cuenta desactivada en {}",
                MDC.get(MdcKeys.TRACE_ID), req.getRequestURI());

        return simple(HttpStatus.UNAUTHORIZED, ErrorCode.AUTHENTICATION_FAILED,
                "Tu cuenta está desactivada. Contacta al administrador.");
    }

    // ── 401: la cookie de sesión no sirve (vencida, revocada o reutilizada) ─────
    @ExceptionHandler(SesionInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleSesionInvalida(SesionInvalidaException ex) {
        return simple(HttpStatus.UNAUTHORIZED, ErrorCode.AUTHENTICATION_FAILED, ex.getMessage());
    }

    // ── 401: credenciales inválidas / usuario deshabilitado ──────────────────
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest req) {

        log.warn("[{}] Fallo de autenticación en {} {}",
                MDC.get(MdcKeys.TRACE_ID),
                req.getMethod(),
                req.getRequestURI());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ErrorCode.AUTHENTICATION_FAILED,
                "Credenciales inválidas",
                MDC.get(MdcKeys.TRACE_ID)
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
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

    private static boolean lanzadaPorAgroRuta(Throwable ex) {
        StackTraceElement[] trace = ex.getStackTrace();
        return trace.length > 0 && trace[0].getClassName().startsWith("com.agroruta.");
    }

    private ResponseEntity<ErrorResponse> simple(HttpStatus status, ErrorCode code, String message) {
        ErrorResponse error = ErrorResponse.of(
                status.value(), code, message, MDC.get(MdcKeys.TRACE_ID));
        return ResponseEntity.status(status).body(error);
    }
}