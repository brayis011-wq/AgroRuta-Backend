package com.agroruta.crop.infrastructure.config;

import com.agroruta.crop.domain.CatalogoCultivoRepository;
import com.agroruta.crop.domain.TipoCultivo;
import com.agroruta.crop.domain.VariedadCultivo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Al arrancar:
 * 1. Convierte siembras.variedad de ENUM (solo uchuva) a texto, si aún es ENUM.
 * 2. Carga el catálogo inicial de cultivos si está vacío.
 * 3. Pasa las siembras antiguas de uchuva al catálogo (tipo y variedad_id).
 * Todo es idempotente: correrlo varias veces no cambia nada.
 */
@Component
@Order(10)
public class CatalogoCultivoInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogoCultivoInicializador.class);

    /** Cultivos comunes en Colombia; el administrador puede agregar o desactivar */
    static final Map<String, List<String>> CATALOGO_INICIAL = new LinkedHashMap<>();
    static {
        CATALOGO_INICIAL.put("Uchuva", List.of("Colombia", "Gigante", "Kenya"));
        CATALOGO_INICIAL.put("Café", List.of("Castillo", "Caturra", "Colombia", "Cenicafé 1"));
        CATALOGO_INICIAL.put("Aguacate", List.of("Hass", "Lorena", "Choquette"));
        CATALOGO_INICIAL.put("Papa", List.of("Pastusa Suprema", "Criolla", "Diacol Capiro"));
        CATALOGO_INICIAL.put("Tomate", List.of("Chonto", "Larga vida"));
        CATALOGO_INICIAL.put("Mora", List.of("Castilla", "Sin espinas"));
        CATALOGO_INICIAL.put("Fresa", List.of("Albión", "Monterrey"));
        CATALOGO_INICIAL.put("Lulo", List.of("Castilla", "La Selva"));
        CATALOGO_INICIAL.put("Plátano", List.of("Hartón", "Dominico"));
        CATALOGO_INICIAL.put("Cacao", List.of("CCN-51", "ICS-95"));
        CATALOGO_INICIAL.put("Maíz", List.of("Amarillo", "Blanco"));
        CATALOGO_INICIAL.put("Fríjol", List.of("Cargamanto", "Bola roja"));
    }

    private final CatalogoCultivoRepository catalogo;
    private final JdbcTemplate jdbc;

    public CatalogoCultivoInicializador(CatalogoCultivoRepository catalogo, JdbcTemplate jdbc) {
        this.catalogo = catalogo;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        convertirColumnaVariedad();
        sembrarCatalogo();
        migrarSiembrasDeUchuva();
    }

    private void convertirColumnaVariedad() {
        try {
            List<String> tipo = jdbc.queryForList(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'siembras' AND COLUMN_NAME = 'variedad'",
                    String.class);
            if (!tipo.isEmpty() && "enum".equalsIgnoreCase(tipo.get(0))) {
                jdbc.execute("ALTER TABLE siembras MODIFY COLUMN variedad VARCHAR(80) NOT NULL");
                log.info("siembras.variedad convertida de ENUM a VARCHAR(80)");
            }
        } catch (Exception e) {
            log.error("No se pudo convertir siembras.variedad a texto. Ejecútalo a mano: "
                    + "ALTER TABLE siembras MODIFY COLUMN variedad VARCHAR(80) NOT NULL", e);
        }
    }

    private void sembrarCatalogo() {
        if (catalogo.contarTipos() > 0) return;
        CATALOGO_INICIAL.forEach((cultivo, variedades) -> {
            TipoCultivo tipo = catalogo.guardarTipo(new TipoCultivo(null, cultivo, true, List.of()));
            variedades.forEach(v -> catalogo.guardarVariedad(
                    new VariedadCultivo(null, v, true, tipo.getId(), tipo.getNombre(), true)));
        });
        log.info("Catálogo de cultivos inicial cargado ({} cultivos)", CATALOGO_INICIAL.size());
    }

    private void migrarSiembrasDeUchuva() {
        try {
            int n = 0;
            for (String v : CATALOGO_INICIAL.get("Uchuva")) {
                n += jdbc.update("UPDATE siembras SET variedad = ? WHERE variedad = ?", v, v.toUpperCase());
            }
            n += jdbc.update("UPDATE siembras SET tipo_cultivo = 'Uchuva' WHERE tipo_cultivo IS NULL");
            n += jdbc.update(
                    "UPDATE siembras s "
                            + "JOIN variedades_cultivo v ON v.nombre = s.variedad "
                            + "JOIN tipos_cultivo t ON t.id = v.tipo_cultivo_id AND t.nombre = s.tipo_cultivo "
                            + "SET s.variedad_id = v.id WHERE s.variedad_id IS NULL");
            if (n > 0) log.info("Siembras antiguas pasadas al catálogo de cultivos ({} cambios)", n);
        } catch (Exception e) {
            log.error("No se pudieron pasar las siembras antiguas al catálogo de cultivos", e);
        }
    }
}
