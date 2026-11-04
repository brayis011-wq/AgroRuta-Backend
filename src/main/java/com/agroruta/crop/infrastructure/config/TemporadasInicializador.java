package com.agroruta.crop.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Al arrancar prepara la base para las temporadas por lote:
 * 1. Quita el índice único de siembras.lote_id (antes un lote solo podía tener una siembra).
 * 2. Ajusta el estado de cada lote según tenga o no una siembra en curso.
 * 3. Registra la etapa de germinación de las siembras que aún no tienen historial de etapas.
 * Todo es idempotente: correrlo varias veces no cambia nada.
 */
@Component
@Order(20)
public class TemporadasInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TemporadasInicializador.class);

    private static final String HAY_SIEMBRA_ACTIVA =
            "EXISTS (SELECT 1 FROM siembras s WHERE s.lote_id = l.id AND s.estado_cultivo <> 'FINALIZADO')";

    private final JdbcTemplate jdbc;

    public TemporadasInicializador(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        quitarUnicoDeLote();
        sincronizarEstadoDeLotes();
        registrarEtapaInicial();
    }

    void quitarUnicoDeLote() {
        try {
            List<String> indices = jdbc.queryForList(
                    "SELECT INDEX_NAME FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'siembras' "
                            + "AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY' "
                            + "GROUP BY INDEX_NAME "
                            + "HAVING COUNT(*) = 1 AND MAX(COLUMN_NAME) = 'lote_id'",
                    String.class);
            for (String indice : indices) {
                jdbc.execute("ALTER TABLE siembras DROP INDEX `" + indice.replace("`", "") + "`");
                log.info("Quitado el índice único {} de siembras.lote_id: los lotes ya pueden tener varias temporadas", indice);
            }
        } catch (Exception e) {
            log.error("No se pudo quitar el índice único de siembras.lote_id. Búscalo con "
                    + "SHOW INDEX FROM siembras y bórralo a mano con ALTER TABLE siembras DROP INDEX <nombre>", e);
        }
    }

    void sincronizarEstadoDeLotes() {
        try {
            int ocupados = jdbc.update(
                    "UPDATE lotes l SET estado = 'EN_CULTIVO' WHERE l.estado <> 'EN_CULTIVO' AND " + HAY_SIEMBRA_ACTIVA);
            int libres = jdbc.update(
                    "UPDATE lotes l SET estado = 'DISPONIBLE' WHERE l.estado = 'EN_CULTIVO' AND NOT " + HAY_SIEMBRA_ACTIVA);
            if (ocupados + libres > 0) {
                log.info("Estado de lotes ajustado: {} en cultivo, {} disponibles", ocupados, libres);
            }
        } catch (Exception e) {
            log.error("No se pudo ajustar el estado de los lotes", e);
        }
    }

    void registrarEtapaInicial() {
        try {
            int n = jdbc.update(
                    "INSERT INTO siembra_etapas (siembra_id, etapa, fecha) "
                            + "SELECT s.id, 'GERMINACION', s.fecha_siembra FROM siembras s "
                            + "WHERE NOT EXISTS (SELECT 1 FROM siembra_etapas e WHERE e.siembra_id = s.id)");
            if (n > 0) log.info("Etapa inicial registrada en {} siembras antiguas", n);
        } catch (Exception e) {
            log.error("No se pudo registrar la etapa inicial de las siembras antiguas", e);
        }
    }
}
