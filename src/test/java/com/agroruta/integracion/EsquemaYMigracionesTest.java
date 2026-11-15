package com.agroruta.integracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Que la aplicación arranque ya dice que Hibernate validó el esquema. Aquí se comprueba además
 * que Flyway aplicó todas las migraciones: FlywayConfig atrapa el fallo de una migración y deja
 * arrancar, así que sin esta comprobación un error pasaría en silencio.
 */
class EsquemaYMigracionesTest extends ConBaseReal {

    @Test
    void sobreUnaBaseVaciaSeAplicanTodasLasMigraciones() throws IOException {
        int archivos = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/migration/V*.sql").length;
        assertTrue(archivos >= 17, "deberían estar al menos las 17 migraciones conocidas; hay " + archivos);

        Integer fallidas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);
        Integer aplicadas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'SQL'", Integer.class);
        Integer ultima = jdbc.queryForObject(
                "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1", Integer.class);

        assertEquals(0, fallidas, "ninguna migración debe quedar fallida");
        assertEquals(archivos, aplicadas, "cada archivo de db/migration debe haberse aplicado");
        assertEquals(archivos, ultima, "la última versión aplicada debe ser la del último archivo");
    }

    @Test
    void lasMigracionesDejanLosInsumosIniciales() {
        Integer insumos = jdbc.queryForObject("SELECT COUNT(*) FROM agricultural_inputs", Integer.class);
        assertTrue(insumos > 0, "V2 carga el catálogo de insumos");
    }

    @Test
    void elDineroEstaEnDecimal() {
        Integer enDecimal = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND column_type = 'decimal(14,2)'
                   AND CONCAT(table_name, '.', column_name) IN ('actividades_cultivo.costo', 'cosechas.precio_kg',
                       'fumigaciones.costo', 'movimientos_inventario.costo_total')
                """, Integer.class);
        assertEquals(4, enDecimal);
    }
}
