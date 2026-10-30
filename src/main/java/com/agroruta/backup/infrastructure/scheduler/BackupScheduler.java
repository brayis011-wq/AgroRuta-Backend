package com.agroruta.backup.infrastructure.scheduler;

import com.agroruta.backup.application.ports.in.CreateBackupUseCase;
import com.agroruta.backup.domain.BackupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "backup.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    private final CreateBackupUseCase createBackup;

    public BackupScheduler(CreateBackupUseCase createBackup) {
        this.createBackup = createBackup;
    }

    // Por defecto todos los días a las 2:00 a.m. (hora Bogotá)
    @Scheduled(cron = "${backup.cron:0 0 2 * * *}", zone = "America/Bogota")
    public void runDailyBackup() {
        try {
            createBackup.create(BackupType.AUTOMATIC);
        } catch (Exception e) {
            // Nunca dejar que una falla tumbe el scheduler
            log.error("Falló el backup automático", e);
        }
    }
}