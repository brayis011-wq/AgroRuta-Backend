package com.agroruta.integracion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Base64;

/**
 * Base de las pruebas que levantan la aplicación completa contra un MySQL real en Docker.
 *
 * Al arrancar, Flyway aplica todas las migraciones sobre una base vacía y Hibernate valida que
 * las tablas coincidan con las entidades: una migración mala o una entidad que no coincide
 * rompe estas pruebas antes de llegar a producción.
 *
 * El contenedor es uno solo para todas las clases (así Spring reutiliza el mismo contexto).
 * Sin Docker las pruebas se saltan; en GitHub Actions siempre hay Docker.
 * Producción es TiDB: lo propio de TiDB se sigue ensayando aparte con su imagen.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
abstract class ConBaseReal {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    /** Secreto solo para esta ejecución; nunca se usa fuera de las pruebas */
    private static final String SECRETO = secretoAlAzar();

    @Autowired
    protected JdbcTemplate jdbc;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        MYSQL.start(); // si ya arrancó, no hace nada
        registro.add("DB_URL", MYSQL::getJdbcUrl);
        registro.add("DB_USERNAME", MYSQL::getUsername);
        registro.add("DB_PASSWORD", MYSQL::getPassword);
        registro.add("JWT_SECRET", () -> SECRETO);
        registro.add("WEATHER_API_KEY", () -> "sin-clave");
        registro.add("BACKUP_SCHEDULER_ENABLED", () -> "false");
    }

    /** Inserta una fila y devuelve su id. */
    protected long insertar(String sql, Object... valores) {
        GeneratedKeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < valores.length; i++) ps.setObject(i + 1, valores[i]);
            return ps;
        }, llave);
        return llave.getKey().longValue();
    }

    private static String secretoAlAzar() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
