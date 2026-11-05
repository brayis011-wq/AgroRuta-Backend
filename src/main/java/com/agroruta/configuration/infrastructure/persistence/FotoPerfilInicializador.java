package com.agroruta.configuration.infrastructure.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Al arrancar amplía usuarios.foto_perfil para que quepa la foto subida desde la app
 * (antes solo guardaba un enlace de 500 caracteres). Es idempotente: si la columna ya es
 * de texto largo no hace nada.
 */
@Component
@Order(30)
public class FotoPerfilInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FotoPerfilInicializador.class);

    private final JdbcTemplate jdbc;

    public FotoPerfilInicializador(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        ampliarColumna();
    }

    void ampliarColumna() {
        try {
            List<String> tipos = jdbc.queryForList(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'foto_perfil'",
                    String.class);
            if (tipos.isEmpty()) {
                return;
            }
            String tipo = tipos.get(0).toLowerCase();
            if (tipo.equals("mediumtext") || tipo.equals("longtext")) {
                return;
            }
            jdbc.execute("ALTER TABLE usuarios MODIFY COLUMN foto_perfil MEDIUMTEXT NULL");
            log.info("Ampliada usuarios.foto_perfil de {} a MEDIUMTEXT para guardar fotos subidas", tipo);
        } catch (Exception e) {
            log.error("No se pudo ampliar usuarios.foto_perfil. Hazlo a mano con: "
                    + "ALTER TABLE usuarios MODIFY COLUMN foto_perfil MEDIUMTEXT NULL", e);
        }
    }
}
