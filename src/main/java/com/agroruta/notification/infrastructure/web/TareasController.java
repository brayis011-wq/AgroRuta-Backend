package com.agroruta.notification.infrastructure.web;

import com.agroruta.notification.application.ports.in.AlertasDiariasUseCase;
import com.agroruta.shared.maintenance.LimpiezaDatosVencidos;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tareas programadas que se disparan desde afuera (GitHub Actions), porque Render gratis
 * se duerme sin visitas. No usan sesión: se protegen con la clave TAREAS_TOKEN.
 */
@RestController
@RequestMapping("/api/jobs")
public class TareasController {

    private final AlertasDiariasUseCase alertas;
    private final LimpiezaDatosVencidos limpieza;
    private final com.agroruta.finance.application.ports.in.PreciosReferenciaUseCase precios;
    private final String token;

    public TareasController(
            AlertasDiariasUseCase alertas,
            LimpiezaDatosVencidos limpieza,
            com.agroruta.finance.application.ports.in.PreciosReferenciaUseCase precios,
            @Value("${tareas.token:}") String token) {
        this.alertas = alertas;
        this.limpieza = limpieza;
        this.precios = precios;
        this.token = token == null ? "" : token.trim();
    }

    @PostMapping("/daily-alerts")
    public ResponseEntity<Map<String, Object>> alertasDiarias(
            @RequestHeader(value = "X-Tarea-Token", required = false) String recibido,
            @RequestParam(defaultValue = "false") boolean forzar) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(token.isEmpty() ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN)
                    .build();
        }
        return ResponseEntity.ok(Map.of("enviados", alertas.enviarResumenDelDia(forzar)));
    }

    /** Borra sesiones y enlaces de correo vencidos; responde cuántas filas quitó de cada tabla */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Integer>> limpiar(
            @RequestHeader(value = "X-Tarea-Token", required = false) String recibido) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(token.isEmpty() ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN)
                    .build();
        }
        return ResponseEntity.ok(limpieza.limpiar());
    }

    /** Precios de SIPSA: solo los trae si los guardados tienen más de 3 días; responde cuántos guardó */
    @PostMapping("/reference-prices")
    public ResponseEntity<Map<String, Integer>> precios(
            @RequestHeader(value = "X-Tarea-Token", required = false) String recibido) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(token.isEmpty() ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN)
                    .build();
        }
        precios.actualizarSiHaceFalta();
        return ResponseEntity.accepted().body(Map.of("pedido", 1));
    }

    /**
     * Precios de SIPSA descargados en un PC en Colombia (el DANE no deja conectar desde Render).
     * El cuerpo es la respuesta SOAP tal cual, o comprimida con gzip. Responde de qué semana quedaron.
     */
    @PostMapping("/reference-prices/upload")
    public ResponseEntity<Map<String, Object>> subirPrecios(
            @RequestHeader(value = "X-Tarea-Token", required = false) String recibido, HttpServletRequest pedido)
            throws IOException {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(token.isEmpty() ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN)
                    .build();
        }
        try (InputStream cuerpo = descomprimir(pedido.getInputStream())) {
            var estado = precios.cargarRespuesta(cuerpo);
            Map<String, Object> r = new java.util.LinkedHashMap<>();
            r.put("semana", String.valueOf(estado.ultimaSemana()));
            r.put("actualizadoEn", String.valueOf(estado.actualizadoEn()));
            return ResponseEntity.ok(r);
        }
    }

    /** Si empieza con la marca de gzip (1f 8b) se descomprime al leer */
    static InputStream descomprimir(InputStream entrada) throws IOException {
        BufferedInputStream b = new BufferedInputStream(entrada);
        b.mark(2);
        int x = b.read(), y = b.read();
        b.reset();
        return x == 0x1f && y == 0x8b ? new GZIPInputStream(b) : b;
    }

    /** Sin clave configurada no se acepta ninguna; la comparación tarda lo mismo acierte o no */
    boolean autorizado(String recibido) {
        if (token.isEmpty() || recibido == null) return false;
        return MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), recibido.trim().getBytes(StandardCharsets.UTF_8));
    }
}
