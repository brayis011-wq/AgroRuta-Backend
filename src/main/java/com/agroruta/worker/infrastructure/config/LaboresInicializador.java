package com.agroruta.worker.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Al arrancar quita el índice único de actividades.nombre: ahora cada agricultor tiene sus labores
 * y dos agricultores pueden llamar igual a una labor suya. Las labores que ya existían quedan como
 * comunes (agricultor_id vacío). Es idempotente: correrlo varias veces no cambia nada.
 */
@Component
@Order(30)
public class LaboresInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LaboresInicializador.class);

    private final JdbcTemplate jdbc;

    public LaboresInicializador(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> indices = jdbc.queryForList(
                    "SELECT INDEX_NAME FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'actividades' "
                            + "AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY' "
                            + "GROUP BY INDEX_NAME "
                            + "HAVING COUNT(*) = 1 AND MAX(COLUMN_NAME) = 'nombre'",
                    String.class);
            for (String indice : indices) {
                jdbc.execute("ALTER TABLE actividades DROP INDEX `" + indice.replace("`", "") + "`");
                log.info("Quitado el índice único {} de actividades.nombre: cada agricultor puede tener sus labores", indice);
            }
        } catch (Exception e) {
            log.error("No se pudo quitar el índice único de actividades.nombre. Búscalo con "
                    + "SHOW INDEX FROM actividades y bórralo a mano con ALTER TABLE actividades DROP INDEX <nombre>", e);
        }
    }
}
