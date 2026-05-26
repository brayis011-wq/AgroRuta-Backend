package com.agroruta.shared.exception;

import com.agroruta.shared.logging.MdcKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    private static final String TRACE_ID = "test-trace-abc-123";

    @BeforeEach
    void setUp() {
        MDC.put(MdcKeys.TRACE_ID, TRACE_ID);
        when(request.getRequestURI()).thenReturn("/api/v1/agricultores");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // ── handleDuplicate ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleDuplicate (DataIntegrityViolationException)")
    class HandleDuplicate {

        @Test
        @DisplayName("debe retornar status 409 CONFLICT")
        void shouldReturn409() {
            DataIntegrityViolationException ex = buildDataIntegrityException("Duplicate entry 'rut-123'");

            ResponseEntity<ErrorResponse> response = handler.handleDuplicate(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("debe retornar errorCode AGR-002 (RESOURCE_ALREADY_EXISTS)")
        void shouldReturnCorrectErrorCode() {
            DataIntegrityViolationException ex = buildDataIntegrityException("Duplicate entry");

            ErrorResponse body = handler.handleDuplicate(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS.getCode());
        }

        @Test
        @DisplayName("debe retornar el mensaje de conflicto fijo")
        void shouldReturnFixedConflictMessage() {
            DataIntegrityViolationException ex = buildDataIntegrityException("any");

            ErrorResponse body = handler.handleDuplicate(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getMessage())
                    .isEqualTo("Ya existe un registro con esos datos. Verifique e intente de nuevo.");
        }

        @Test
        @DisplayName("debe incluir el traceId del MDC en la respuesta")
        void shouldIncludeTraceId() {
            DataIntegrityViolationException ex = buildDataIntegrityException("any");

            ErrorResponse body = handler.handleDuplicate(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getTraceId()).isEqualTo(TRACE_ID);
        }

        @Test
        @DisplayName("debe incluir status 409 dentro del body")
        void shouldIncludeStatusInBody() {
            DataIntegrityViolationException ex = buildDataIntegrityException("any");

            ErrorResponse body = handler.handleDuplicate(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        private DataIntegrityViolationException buildDataIntegrityException(String causeMessage) {
            DataIntegrityViolationException ex = mock(DataIntegrityViolationException.class);
            Throwable cause = new RuntimeException(causeMessage);
            when(ex.getMostSpecificCause()).thenReturn(cause);
            return ex;
        }
    }

    // ── handleNotFound ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleNotFound (ResourceNotFoundException)")
    class HandleNotFound {

        @Test
        @DisplayName("debe retornar status 404 NOT_FOUND")
        void shouldReturn404() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Agricultor", 5L);

            ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("debe retornar el errorCode del enum de la excepción")
        void shouldReturnExceptionErrorCode() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Predio", 99L);

            ErrorResponse body = handler.handleNotFound(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("debe retornar el mensaje generado por la excepción")
        void shouldReturnExceptionMessage() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Cultivo", 7L);

            ErrorResponse body = handler.handleNotFound(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getMessage()).isEqualTo("Cultivo con id '7' no fue encontrado");
        }

        @Test
        @DisplayName("debe incluir el traceId del MDC")
        void shouldIncludeTraceId() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Lote", 1L);

            ErrorResponse body = handler.handleNotFound(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getTraceId()).isEqualTo(TRACE_ID);
        }

        @Test
        @DisplayName("debe propagar el errorCode personalizado si la excepción lo trae")
        void shouldPropagateCustomErrorCode() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS, "Código duplicado detectado");

            ErrorResponse body = handler.handleNotFound(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS.getCode());
        }
    }

    // ── handleBusiness ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleBusiness (BusinessException)")
    class HandleBusiness {

        @Test
        @DisplayName("debe retornar status 422 UNPROCESSABLE_ENTITY")
        void shouldReturn422() {
            BusinessException ex = new BusinessException("Stock insuficiente para el despacho");

            ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @Test
        @DisplayName("debe retornar el errorCode del enum de la excepción")
        void shouldReturnExceptionErrorCode() {
            BusinessException ex = new BusinessException(ErrorCode.INVALID_OPERATION);

            ErrorResponse body = handler.handleBusiness(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPERATION.getCode());
        }

        @Test
        @DisplayName("debe retornar el mensaje de la excepción")
        void shouldReturnExceptionMessage() {
            BusinessException ex = new BusinessException("No se puede cerrar un ciclo abierto");

            ErrorResponse body = handler.handleBusiness(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getMessage()).isEqualTo("No se puede cerrar un ciclo abierto");
        }

        @Test
        @DisplayName("debe incluir el traceId del MDC")
        void shouldIncludeTraceId() {
            BusinessException ex = new BusinessException("Regla violada");

            ErrorResponse body = handler.handleBusiness(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getTraceId()).isEqualTo(TRACE_ID);
        }

        @Test
        @DisplayName("debe retornar 422 en el body")
        void shouldInclude422InBody() {
            BusinessException ex = new BusinessException("error");

            ErrorResponse body = handler.handleBusiness(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        }
    }

    // ── handleValidation ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleValidation (MethodArgumentNotValidException)")
    class HandleValidation {

        @Test
        @DisplayName("debe retornar status 400 BAD_REQUEST")
        void shouldReturn400() {
            MethodArgumentNotValidException ex = buildValidationException(
                    List.of(fieldError("nombre", "No puede estar vacío")));

            ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("debe retornar errorCode AGR-021 (VALIDATION_FAILED)")
        void shouldReturnValidationFailedCode() {
            MethodArgumentNotValidException ex = buildValidationException(
                    List.of(fieldError("rut", "Formato inválido")));

            ErrorResponse body = handler.handleValidation(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.getCode());
        }

        @Test
        @DisplayName("debe retornar el mensaje de validación fijo")
        void shouldReturnFixedValidationMessage() {
            MethodArgumentNotValidException ex = buildValidationException(
                    List.of(fieldError("campo", "error")));

            ErrorResponse body = handler.handleValidation(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getMessage()).isEqualTo("Error de validación en los datos enviados.");
        }

        @Test
        @DisplayName("debe incluir los fieldErrors en el body")
        void shouldIncludeFieldErrors() {
            MethodArgumentNotValidException ex = buildValidationException(List.of(
                    fieldError("nombre", "No puede estar vacío"),
                    fieldError("rut", "Formato inválido")));

            ErrorResponse body = handler.handleValidation(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getFieldErrors())
                    .containsEntry("nombre", "No puede estar vacío")
                    .containsEntry("rut", "Formato inválido");
        }

        @Test
        @DisplayName("ante dos errores en el mismo campo debe conservar el primero (merge strategy)")
        void shouldKeepFirstErrorWhenSameFieldHasMultiple() {
            MethodArgumentNotValidException ex = buildValidationException(List.of(
                    fieldError("email", "primer error"),
                    fieldError("email", "segundo error que debe ignorarse")));

            ErrorResponse body = handler.handleValidation(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getFieldErrors()).containsEntry("email", "primer error");
        }

        @Test
        @DisplayName("debe incluir el traceId del MDC")
        void shouldIncludeTraceId() {
            MethodArgumentNotValidException ex = buildValidationException(
                    List.of(fieldError("campo", "error")));

            ErrorResponse body = handler.handleValidation(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getTraceId()).isEqualTo(TRACE_ID);
        }

        @Test
        @DisplayName("debe manejar una lista vacía de errores de campo")
        void shouldHandleEmptyFieldErrors() {
            MethodArgumentNotValidException ex = buildValidationException(List.of());

            ErrorResponse body = handler.handleValidation(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getFieldErrors()).isEmpty();
        }

        private MethodArgumentNotValidException buildValidationException(List<FieldError> fieldErrors) {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
            return ex;
        }

        private FieldError fieldError(String field, String message) {
            FieldError fe = mock(FieldError.class);
            when(fe.getField()).thenReturn(field);
            when(fe.getDefaultMessage()).thenReturn(message);
            return fe;
        }
    }

    // ── handleIllegalArgument ────────────────────────────────────────────────

    @Nested
    @DisplayName("handleIllegalArgument (IllegalArgumentException)")
    class HandleIllegalArgument {

        @Test
        @DisplayName("debe retornar status 400 BAD_REQUEST")
        void shouldReturn400() {
            IllegalArgumentException ex = new IllegalArgumentException("Tipo de cultivo desconocido: XYZW");

            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("debe retornar errorCode AGR-020 (INVALID_ARGUMENT)")
        void shouldReturnInvalidArgumentCode() {
            IllegalArgumentException ex = new IllegalArgumentException("argumento inválido");

            ErrorResponse body = handler.handleIllegalArgument(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT.getCode());
        }

        @Test
        @DisplayName("debe retornar el mensaje de la excepción")
        void shouldReturnExceptionMessage() {
            IllegalArgumentException ex = new IllegalArgumentException("El id no puede ser negativo");

            ErrorResponse body = handler.handleIllegalArgument(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getMessage()).isEqualTo("El id no puede ser negativo");
        }

        @Test
        @DisplayName("debe incluir el traceId del MDC")
        void shouldIncludeTraceId() {
            IllegalArgumentException ex = new IllegalArgumentException("error");

            ErrorResponse body = handler.handleIllegalArgument(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getTraceId()).isEqualTo(TRACE_ID);
        }
    }

    // ── handleExternal ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleExternal (ExternalApiException)")
    class HandleExternal {

        @Test
        @DisplayName("debe retornar status 502 BAD_GATEWAY")
        void shouldReturn502() {
            ExternalApiException ex = new ExternalApiException("Timeout conectando con SIIGA");

            ResponseEntity<ErrorResponse> response = handler.handleExternal(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        }

        @Test
        @DisplayName("debe retornar errorCode AGR-501 (EXTERNAL_SERVICE_ERROR)")
        void shouldReturnExternalServiceErrorCode() {
            ExternalApiException ex = new ExternalApiException("Error externo");

            ErrorResponse body = handler.handleExternal(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode());
        }

        @Test
        @DisplayName("debe retornar el mensaje de la excepción")
        void shouldReturnExceptionMessage() {
            ExternalApiException ex = new ExternalApiException("API de pagos no disponible");

            ErrorResponse body = handler.handleExternal(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getMessage()).isEqualTo("API de pagos no disponible");
        }

        @Test
        @DisplayName("debe incluir el traceId del MDC")
        void shouldIncludeTraceId() {
            ExternalApiException ex = new ExternalApiException("error externo");

            ErrorResponse body = handler.handleExternal(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getTraceId()).isEqualTo(TRACE_ID);
        }

        @Test
        @DisplayName("debe funcionar cuando la excepción trae causa encadenada")
        void shouldWorkWithCause() {
            Throwable cause = new RuntimeException("Connection refused");
            ExternalApiException ex = new ExternalApiException("Error en AGROSAVIA", cause);

            ResponseEntity<ErrorResponse> response = handler.handleExternal(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        }
    }

    // ── handleGeneral ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleGeneral (Exception catch-all)")
    class HandleGeneral {

        @Test
        @DisplayName("debe retornar status 500 INTERNAL_SERVER_ERROR")
        void shouldReturn500() {
            Exception ex = new RuntimeException("fallo inesperado en el scheduler");

            ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("debe retornar errorCode AGR-500 (INTERNAL_ERROR)")
        void shouldReturnInternalErrorCode() {
            Exception ex = new RuntimeException("error genérico");

            ErrorResponse body = handler.handleGeneral(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        }

        @Test
        @DisplayName("el mensaje del body debe incluir el traceId como referencia para soporte")
        void shouldIncludeTraceIdInMessage() {
            Exception ex = new RuntimeException("error");

            ErrorResponse body = handler.handleGeneral(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getMessage()).contains(TRACE_ID);
        }

        @Test
        @DisplayName("el traceId en el campo traceId del body debe coincidir con el del MDC")
        void shouldIncludeTraceIdInField() {
            Exception ex = new RuntimeException("error");

            ErrorResponse body = handler.handleGeneral(ex, request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getTraceId()).isEqualTo(TRACE_ID);
        }

        @Test
        @DisplayName("debe capturar cualquier subtipo de Exception")
        void shouldCatchAnyExceptionSubtype() {
            when(request.getMethod()).thenReturn("POST");

            Exception ex = new NullPointerException("null en servicio de liquidación");

            ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("el body debe tener status 500")
        void shouldInclude500InBody() {
            when(request.getMethod()).thenReturn("GET");

            ErrorResponse body = handler.handleGeneral(new RuntimeException("err"), request).getBody();

            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // ── Comportamiento del MDC ───────────────────────────────────────────────

    @Nested
    @DisplayName("Comportamiento cuando el MDC no tiene traceId")
    class MdcAbsent {

        @BeforeEach
        void clearMdc() {
            MDC.clear(); // sobreescribe el setUp del padre
            when(request.getRequestURI()).thenReturn("/api/v1/test");
        }

        @Test
        @DisplayName("handleNotFound debe funcionar sin lanzar excepción cuando el MDC está vacío")
        void handleNotFoundShouldNotFailWithoutMdc() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Entidad", 1L);

            ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTraceId()).isNull();
        }

        @Test
        @DisplayName("handleGeneral debe funcionar sin lanzar excepción cuando el MDC está vacío")
        void handleGeneralShouldNotFailWithoutMdc() {
            when(request.getMethod()).thenReturn("DELETE");
            Exception ex = new RuntimeException("error sin traceId");

            ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}