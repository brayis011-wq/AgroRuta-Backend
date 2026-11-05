package com.agroruta.shared.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Migraciones de base de datos con Flyway, en todos los entornos (local y producción).
 *
 * Flyway es el dueño del esquema: corre antes que Hibernate, y Hibernate solo valida
 * (ddl-auto=validate) que las tablas coincidan con las entidades.
 *  - Base vacía: V1 crea las tablas y V2 carga los insumos.
 *  - Base con tablas pero sin historial: se anota una línea base para no repetir lo que ya existe
 *    (V2 si ya hay insumos, porque los duplicaría; V1 si no hay, para que V2 los cargue).
 *  - Bases que ya tienen historial (producción): siguen desde donde iban.
 * Todo cambio a una entidad necesita su migración V3__..., V4__... en db/migration;
 * si falta, la app no arranca y el error dice qué tabla o columna no coincide.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    /** V1 = esquema inicial, V2 = insumos iniciales. */
    static final String LINEA_BASE_NUEVA = "1";
    static final String LINEA_BASE_CON_DATOS = "2";

    /** Hibernate (entityManagerFactory) espera a que Flyway termine, para validar el esquema ya migrado. */
    @Bean
    public static BeanFactoryPostProcessor jpaDespuesDeFlyway() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) return;
            BeanDefinition emf = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] previas = emf.getDependsOn();
            String[] todas = previas == null ? new String[1] : Arrays.copyOf(previas, previas.length + 1);
            todas[todas.length - 1] = "flyway";
            emf.setDependsOn(todas);
        };
    }

    @Bean
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
            // Si el esquema igual coincide con las entidades, la app arranca; si no, Hibernate lo dirá al validar.
            log.error("Flyway no pudo aplicar las migraciones. Revisa la tabla flyway_schema_history.", e);
        }
        return flyway;
    }

    /**
     * TiDB no soporta "CREATE TABLE ... SELECT", que es lo que usa Flyway para crear su tabla de historial
     * con la línea base. Por eso la creamos nosotros, con la misma estructura que usa Flyway en MySQL,
     * y anotamos la línea base. Si la tabla ya existe no se toca.
     * En una base vacía no hay línea base: la tabla queda sin filas y Flyway corre desde V1.
     */
    static void prepararHistorial(DataSource dataSource, String lineaBase) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            if (existeHistorial(c)) return;
            try (Statement st = c.createStatement()) {
                st.execute(CREAR_HISTORIAL);
                st.execute("CREATE INDEX `flyway_schema_history_s_idx` ON `flyway_schema_history` (`success`)");
            }
            if (contarTablas(c) <= 1) { // solo la de historial que acabamos de crear
                log.info("Base vacía: creada la tabla flyway_schema_history; Flyway aplicará desde V1");
                return;
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

    private static long contarTablas(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
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
