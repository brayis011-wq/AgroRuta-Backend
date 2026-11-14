package com.agroruta.shared.maintenance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Borra lo que ya no sirve de las tablas que solo crecen: sesiones, enlaces de correo y el
 * contador diario de diagnósticos. La dispara la tarea diaria de GitHub Actions
 * (POST /api/jobs/cleanup), porque Render gratis se duerme y no correría un @Scheduled.
 *
 * Los márgenes son de días a propósito: las fechas de estas tablas se escriben con relojes
 * distintos (el del servidor y el de Colombia) y así unas horas de diferencia no importan.
 */
@Component
public class LimpiezaDatosVencidos {

    /** Una sesión vencida ya no sirve para nada; el día de margen cubre la diferencia de relojes */
    static final Duration MARGEN_VENCIDAS = Duration.ofDays(1);
    /**
     * Cada renovación (cada 15 minutos de uso) deja una fila reemplazada. Se conservan unos días
     * porque sirven para notar un token robado: si alguien usa uno ya reemplazado, se cierran
     * todas las sesiones de la cuenta. Pasado este tiempo ese token simplemente no entra.
     */
    static final Duration MARGEN_REEMPLAZADAS = Duration.ofDays(7);
    /** Enlaces de verificar correo y de recuperar contraseña: se guardan unos días por si hay que revisar un caso */
    static final Duration MARGEN_ENLACES = Duration.ofDays(7);
    /** El contador de diagnósticos solo se consulta para el día de hoy */
    static final int DIAS_USO_DIAGNOSTICO = 30;

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    @Autowired
    public LimpiezaDatosVencidos(JdbcTemplate jdbc) {
        this(jdbc, Clock.system(ZoneId.of("America/Bogota")));
    }

    LimpiezaDatosVencidos(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    /** Devuelve cuántas filas se borraron de cada tabla */
    public Map<String, Integer> limpiar() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime reemplazadas = ahora.minus(MARGEN_REEMPLAZADAS);
        LocalDateTime enlaces = ahora.minus(MARGEN_ENLACES);

        Map<String, Integer> borrados = new LinkedHashMap<>();
        borrados.put("sesiones", jdbc.update(
                "DELETE FROM sesiones WHERE expira_en < ? OR rotado_en < ? OR revocado_en < ?",
                ahora.minus(MARGEN_VENCIDAS), reemplazadas, reemplazadas));
        borrados.put("verificacionesCorreo", jdbc.update(
                "DELETE FROM verificaciones_correo WHERE expira_en < ?", enlaces));
        borrados.put("restablecimientosContrasena", jdbc.update(
                "DELETE FROM restablecimientos_contrasena WHERE expira_en < ?", enlaces));
        borrados.put("usoDiagnosticos", jdbc.update(
                "DELETE FROM diagnosticos_uso WHERE dia < ?",
                Date.valueOf(ahora.toLocalDate().minusDays(DIAS_USO_DIAGNOSTICO))));
        return borrados;
    }
}
