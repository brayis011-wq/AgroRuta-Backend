package com.agroruta.shared.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Migraciones de base de datos con Flyway, en todos los entornos (local y producción).
 *
 * Por ahora Hibernate sigue creando/ajustando tablas (ddl-auto=update) y Flyway corre después:
 *  - Base nueva: Hibernate crea las tablas vacías, la línea base queda en V1 y V2 carga los insumos.
 *  - Base con datos sin historial (producción): la línea base queda en V2, así V1 y V2 no se
 *    vuelven a correr (V2 duplicaría los insumos) y desde V3 todo cambio queda registrado.
 *  - Bases locales que ya tenían historial con línea base 1: siguen igual; V1 queda debajo de su línea base.
 * Los cambios nuevos al esquema van como V3__..., V4__... en db/migration.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    /** V1 = esquema inicial, V2 = insumos iniciales. */
    static final String LINEA_BASE_NUEVA = "1";
    static final String LINEA_BASE_CON_DATOS = "2";

    @Bean
    @DependsOn("entityManagerFactory") // Hibernate primero; Flyway después
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(lineaBase(dataSource))
                .load();

        try {
            flyway.migrate();
        } catch (RuntimeException e) {
            // Mientras Hibernate siga manejando el esquema, un fallo de Flyway no debe tumbar la app.
            log.error("Flyway no pudo aplicar las migraciones; la app sigue con el esquema de Hibernate. "
                    + "Revisa la tabla flyway_schema_history.", e);
        }
        return flyway;
    }

    /** Si ya hay insumos cargados, la base tiene datos y V2 no se debe repetir. */
    static String lineaBase(DataSource dataSource) {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM agricultural_inputs")) {
            return rs.next() && rs.getLong(1) > 0 ? LINEA_BASE_CON_DATOS : LINEA_BASE_NUEVA;
        } catch (Exception e) {
            return LINEA_BASE_NUEVA; // sin la tabla todavía: base nueva
        }
    }
}
