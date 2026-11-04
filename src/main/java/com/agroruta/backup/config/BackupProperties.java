package com.agroruta.backup.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "backup")
public class BackupProperties {

    /** Carpeta donde se guardan los .sql (debe ser un volumen persistente en Docker). */
    private String directory = "backups";

    /** Binarios del cliente MySQL/MariaDB (deben estar en el PATH de la imagen). */
    private String mysqldumpPath = "mysqldump";
    private String mysqlPath = "mysql";

    /** Tiempo máximo de una operación de dump/restore. */
    private int timeoutMinutes = 10;
    /** Opción de SSL para el cliente. Vacío = comportamiento por defecto del cliente.
     *  MySQL: --ssl-mode=DISABLED | MariaDB (Docker/Alpine): --skip-ssl */
    private String sslArg = "";

    /**
     * true: dump consistente con --single-transaction (MySQL).
     * false: sin transacción ni bloqueos, necesario en TiDB, que rechaza el
     * "ROLLBACK TO SAVEPOINT" que usa mysqldump dentro de --single-transaction.
     */
    private boolean singleTransaction = true;
}