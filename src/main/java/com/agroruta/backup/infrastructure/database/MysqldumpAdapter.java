package com.agroruta.backup.infrastructure.database;

import com.agroruta.backup.domain.BackupFailedException;
import com.agroruta.backup.application.ports.out.DatabaseDumpPort;
import com.agroruta.backup.config.BackupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta mysqldump / mysql como procesos externos.
 * Requiere el cliente de MySQL 8 (en Docker se instala con apt: mysql-client).
 * Las opciones del dump son compatibles con MySQL 8 y con TiDB.
 * La contraseña se pasa por la variable de entorno MYSQL_PWD (nunca por argumentos).
 */
@Component
public class MysqldumpAdapter implements DatabaseDumpPort {

    private static final Logger log = LoggerFactory.getLogger(MysqldumpAdapter.class);

    private final BackupProperties properties;
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String database;

    public MysqldumpAdapter(BackupProperties properties,
                            @Value("${spring.datasource.url}") String jdbcUrl,
                            @Value("${spring.datasource.username}") String username,
                            @Value("${spring.datasource.password}") String password) {
        this.properties = properties;
        this.username = username;
        this.password = password;

        // jdbc:mysql://host:3306/db?params  ->  mysql://host:3306/db?params
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        this.host = uri.getHost();
        this.port = uri.getPort() == -1 ? 3306 : uri.getPort();
        this.database = uri.getPath().substring(1);
    }

    @Override
    public void dump(Path target) {
        List<String> command = new ArrayList<>(List.of(
                properties.getMysqldumpPath(),
                "--host=" + host,
                "--port=" + port,
                "--user=" + username,
                "--single-transaction",
                "--no-tablespaces",
                // Compatibilidad con TiDB (también funcionan con MySQL 8):
                "--column-statistics=0",   // TiDB no tiene information_schema.COLUMN_STATISTICS
                "--set-gtid-purged=OFF",   // TiDB no usa GTID
                "--skip-triggers",         // TiDB no soporta triggers (la app no los usa)
                "--skip-add-locks"));      // evita LOCK TABLES al restaurar
        addSslArg(command);
        command.add("--result-file=" + target);
        command.add(database);
        try {
            run(command, null);
        } catch (RuntimeException e) {
            deleteQuietly(target); // no dejar dumps a medias
            throw e;
        }
    }

    @Override
    public void restore(Path source) {
        List<String> command = new ArrayList<>(List.of(
                properties.getMysqlPath(),
                "--host=" + host,
                "--port=" + port,
                "--user=" + username));
        addSslArg(command);
        command.add(database);
        run(command, source);
    }

    private void addSslArg(List<String> command) {
        String sslArg = properties.getSslArg();
        if (sslArg != null && !sslArg.isBlank()) {
            command.add(sslArg.trim());
        }
    }

    private void run(List<String> command, Path stdin) {
        Path logFile = null;
        try {
            logFile = Files.createTempFile("agroruta-backup-", ".log");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("MYSQL_PWD", password);
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile.toFile());
            if (stdin != null) {
                pb.redirectInput(stdin.toFile());
            }

            Process process = pb.start();
            boolean finished = process.waitFor(properties.getTimeoutMinutes(), TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                throw new BackupFailedException("Tiempo de espera agotado ejecutando " + command.get(0));
            }
            if (process.exitValue() != 0) {
                String output = Files.readString(logFile);
                log.error("{} terminó con código {}: {}", command.get(0), process.exitValue(), output);
                throw new BackupFailedException("Falló la ejecución de " + command.get(0));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo ejecutar " + command.get(0), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackupFailedException("Operación de backup interrumpida", e);
        } finally {
            deleteQuietly(logFile);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // nada que hacer
        }
    }
}