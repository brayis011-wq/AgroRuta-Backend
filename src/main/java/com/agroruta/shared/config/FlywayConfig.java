package com.agroruta.shared.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
        String lineaBase = lineaBase(dataSource);
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(lineaBase)
                .load();

        try {
            prepararHistorial(dataSource, lineaBase);
        } catch (Exception e) {
            log.warn("No se pudo preparar flyway_schema_history; Flyway lo intentará por su cuenta", e);
        }
        try {
            flyway.migrate();
        } catch (RuntimeException e) {
            // Mientras Hibernate siga manejando el esquema, un fallo de Flyway no debe tumbar la app.
            log.error("Flyway no pudo aplicar las migraciones; la app sigue con el esquema de Hibernate. "
                    + "Revisa la tabla flyway_schema_history.", e);
        }
        return flyway;
    }

    /**
     * TiDB no soporta "CREATE TABLE ... SELECT", que es lo que usa Flyway para crear su tabla de historial
     * con la línea base. Por eso la creamos nosotros, con la misma estructura que usa Flyway en MySQL,
     * y anotamos la línea base. Si la tabla ya existe no se toca.
     */
    static void prepararHistorial(DataSource dataSource, String lineaBase) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            if (existeHistorial(c)) return;
            try (Statement st = c.createStatement()) {
                st.execute(CREAR_HISTORIAL);
                st.execute("CREATE INDEX `flyway_schema_history_s_idx` ON `flyway_schema_history` (`success`)");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, "
                            + "`checksum`, `installed_by`, `execution_time`, `success`) "
                            + "VALUES (1, ?, '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'agroruta', 0, 1)")) {
                ps.setString(1, lineaBase);
                ps.executeUpdate();
            }
            log.info("Creada la tabla flyway_schema_history con línea base en V{}", lineaBase);
        }
    }

    private static boolean existeHistorial(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = 'flyway_schema_history'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getLong(1) > 0;
        }
    }

    /** Misma estructura que crea Flyway 10 en MySQL */
    static final String CREAR_HISTORIAL = """
            CREATE TABLE `flyway_schema_history` (
                `installed_rank` INT NOT NULL,
                `version` VARCHAR(50),
                `description` VARCHAR(200) NOT NULL,
                `type` VARCHAR(20) NOT NULL,
                `script` VARCHAR(1000) NOT NULL,
                `checksum` INT,
                `installed_by` VARCHAR(100) NOT NULL,
                `installed_on` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                `execution_time` INT NOT NULL,
                `success` BOOL NOT NULL,
                CONSTRAINT `flyway_schema_history_pk` PRIMARY KEY (`installed_rank`)
            ) ENGINE=InnoDB""";

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
