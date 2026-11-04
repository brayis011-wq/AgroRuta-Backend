package com.agroruta.backup.domain;

/**
 * Falla técnica al generar o restaurar un backup (mysqldump, tiempo de espera, etc.).
 * No es un error del cliente: el GlobalExceptionHandler la responde como 500.
 */
public class BackupFailedException extends RuntimeException {

    public BackupFailedException(String message) {
        super(message);
    }

    public BackupFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
