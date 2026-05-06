// shared/logging/RequestLoggingFilter.java
package com.agroruta.shared.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1) // se ejecuta primero
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Genera o propaga el traceId (útil si Angular lo envía en el header)
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        long startTime = System.currentTimeMillis();

        // Poblar MDC — todo log dentro de este request tendrá este contexto
        MDC.put(MdcKeys.TRACE_ID,    traceId);
        MDC.put(MdcKeys.HTTP_METHOD, request.getMethod());
        MDC.put(MdcKeys.REQUEST_URI, request.getRequestURI());

        // Exponer el traceId al cliente Angular
        response.setHeader("X-Trace-Id", traceId);

        log.info("→ {} {}", request.getMethod(), request.getRequestURI());

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put(MdcKeys.RESPONSE_CODE, String.valueOf(response.getStatus()));
            MDC.put(MdcKeys.DURATION_MS,   String.valueOf(duration));

            log.info("← {} {} | status={} | {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);

            MDC.clear(); // MUY IMPORTANTE: evita memory leaks en thread pools
        }
    }
}