package com.agroruta.shared.web;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class EnviosRecibidosJdbc implements EnviosRecibidos {

    /** Un teléfono no guarda un envío pendiente más de unos días; pasado este tiempo la clave ya no hace falta */
    private static final Duration VIGENCIA = Duration.ofDays(30);

    private final JdbcTemplate jdbc;

    public EnviosRecibidosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean anotar(Long usuarioId, String clave) {
        try {
            jdbc.update("INSERT INTO envios_recibidos (usuario_id, clave, recibido_en) VALUES (?, ?, ?)",
                    usuarioId, clave, Timestamp.from(Instant.now()));
        } catch (DuplicateKeyException repetida) {
            return false;
        }
        limpiarDeVezEnCuando();
        return true;
    }

    @Override
    public void olvidar(Long usuarioId, String clave) {
        jdbc.update("DELETE FROM envios_recibidos WHERE usuario_id = ? AND clave = ?", usuarioId, clave);
    }

    /** Sin tarea programada (Render gratis se duerme): uno de cada 50 envíos barre las claves viejas */
    private void limpiarDeVezEnCuando() {
        if (ThreadLocalRandom.current().nextInt(50) != 0) return;
        try {
            jdbc.update("DELETE FROM envios_recibidos WHERE recibido_en < ?",
                    Timestamp.from(Instant.now().minus(VIGENCIA)));
        } catch (RuntimeException e) {
            // La limpieza es de mantenimiento: si falla no debe tumbar el registro que sí se guardó
        }
    }
}
