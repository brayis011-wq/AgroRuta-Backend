package com.agroruta.backup.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadatos de un archivo de backup. El tipo viaja en el nombre del archivo:
 * agroruta_{manual|auto|prerestore}_yyyyMMdd_HHmmss.sql
 * Solo se aceptan nombres con ese formato (evita path traversal).
 */
public record Backup(String name, long sizeBytes, Instant createdAt, BackupType type) {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^agroruta_(manual|auto|prerestore)_\\d{8}_\\d{6}\\.sql$");

    public static String generateName(BackupType type, LocalDateTime when) {
        return "agroruta_" + type.getCode() + "_" + TIMESTAMP.format(when) + ".sql";
    }

    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    public static BackupType typeOf(String name) {
        Matcher matcher = NAME_PATTERN.matcher(name == null ? "" : name);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Nombre de backup inválido: " + name);
        }
        return BackupType.fromCode(matcher.group(1));
    }
}