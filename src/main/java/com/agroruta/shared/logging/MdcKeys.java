// shared/logging/MdcKeys.java
package com.agroruta.shared.logging;

/**
 * Claves estándar usadas en el MDC (Mapped Diagnostic Context).
 * Permiten agregar contexto a cada línea de log automáticamente.
 */
public final class MdcKeys {

    private MdcKeys() {}

    public static final String TRACE_ID      = "traceId";
    public static final String USER_ID       = "userId";
    public static final String HTTP_METHOD   = "httpMethod";
    public static final String REQUEST_URI   = "requestUri";
    public static final String RESPONSE_CODE = "responseCode";
    public static final String DURATION_MS   = "durationMs";
}