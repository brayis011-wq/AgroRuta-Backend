package com.agroruta.notification.infrastructure.web;

import com.agroruta.notification.application.ports.in.AlertasDiariasUseCase;
import com.agroruta.shared.maintenance.LimpiezaDatosVencidos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Tareas programadas que se disparan desde afuera (GitHub Actions), porque Render gratis
 * se duerme sin visitas. No usan sesión: se protegen con la clave TAREAS_TOKEN.
 */
@RestController
@RequestMapping("/api/jobs")
public class TareasController {

    private final AlertasDiariasUseCase alertas;
    private final LimpiezaDatosVencidos limpieza;
    private final String token;

    public TareasController(AlertasDiariasUseCase alertas, LimpiezaDatosVencidos limpieza,
                            @Value("${tareas.token:}") String token) {
        this.alertas = alertas;
        this.limpieza = limpieza;
        this.token = token == null ? "" : token.trim();
    }

    @PostMapping("/daily-alerts")
    public ResponseEntity<Map<String, Object>> alertasDiarias(
            @RequestHeader(value = "X-Tarea-Token", required = false) String recibido,
            @RequestParam(defaultValue = "false") boolean forzar) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(token.isEmpty() ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(Map.of("enviados", alertas.enviarResumenDelDia(forzar)));
    }

    /** Borra sesiones y enlaces de correo vencidos; responde cuántas filas quitó de cada tabla */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Integer>> limpiar(
            @RequestHeader(value = "X-Tarea-Token", required = false) String recibido) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(token.isEmpty() ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(limpieza.limpiar());
    }

    /** Sin clave configurada no se acepta ninguna; la comparación tarda lo mismo acierte o no */
    boolean autorizado(String recibido) {
        if (token.isEmpty() || recibido == null) return false;
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), recibido.trim().getBytes(StandardCharsets.UTF_8));
    }
}
