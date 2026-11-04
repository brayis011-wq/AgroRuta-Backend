package com.agroruta.backup.application;

import com.agroruta.backup.domain.BackupFailedException;
import com.agroruta.backup.application.ports.in.CreateBackupUseCase;
import com.agroruta.backup.application.ports.in.DeleteBackupUseCase;
import com.agroruta.backup.application.ports.in.DownloadBackupUseCase;
import com.agroruta.backup.application.ports.in.ListBackupsUseCase;
import com.agroruta.backup.application.ports.in.RestoreBackupUseCase;
import com.agroruta.backup.application.ports.out.BackupStoragePort;
import com.agroruta.backup.application.ports.out.DatabaseDumpPort;
import com.agroruta.backup.domain.Backup;
import com.agroruta.backup.domain.BackupType;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class BackupService implements CreateBackupUseCase,
        ListBackupsUseCase,
        DownloadBackupUseCase,
        DeleteBackupUseCase,
        RestoreBackupUseCase {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final DatabaseDumpPort dumpPort;
    private final BackupStoragePort storage;
    private final int retention;

    // Evita backups/restauraciones simultáneos
    private final ReentrantLock lock = new ReentrantLock();

    public BackupService(DatabaseDumpPort dumpPort,
                         BackupStoragePort storage,
                         @Value("${backup.retention:10}") int retention) {
        this.dumpPort = dumpPort;
        this.storage = storage;
        this.retention = retention;
    }

    @Override
    public Backup create(BackupType type) {
        acquireLock();
        try {
            return doCreate(type);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Backup> list() {
        return storage.findAll();
    }

    @Override
    public Path getFile(String name) {
        findOrThrow(name);
        return storage.pathOf(name);
    }

    @Override
    public void delete(String name) {
        findOrThrow(name);
        acquireLock();
        try {
            storage.delete(name);
            log.info("Backup eliminado: {}", name);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void restore(String name) {
        findOrThrow(name);
        acquireLock();
        try {
            // Red de seguridad: respaldo del estado actual antes de sobrescribir
            Backup safety = doCreate(BackupType.PRE_RESTORE);
            log.warn("Restaurando base de datos desde {} (respaldo previo: {})", name, safety.name());
            dumpPort.restore(storage.pathOf(name));
            log.warn("Restauración completada desde {}", name);
        } finally {
            lock.unlock();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Backup doCreate(BackupType type) {
        String name = Backup.generateName(type, LocalDateTime.now());

        if (storage.findByName(name).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Ya se generó un backup en este instante, intenta de nuevo");
        }

        dumpPort.dump(storage.pathOf(name));

        Backup backup = storage.findByName(name)
                .orElseThrow(() -> new BackupFailedException("El backup no se generó correctamente: " + name));

        log.info("Backup {} creado: {} ({} bytes)", type, backup.name(), backup.sizeBytes());

        if (type == BackupType.AUTOMATIC) {
            applyRetention();
        }
        return backup;
    }

    /** Conserva solo los últimos N backups automáticos. Los manuales y pre-restore no se tocan. */
    private void applyRetention() {
        List<Backup> automaticos = storage.findAll().stream()
                .filter(b -> b.type() == BackupType.AUTOMATIC)
                .toList(); // ya vienen del más reciente al más antiguo

        automaticos.stream().skip(retention).forEach(old -> {
            storage.delete(old.name());
            log.info("Retención: eliminado backup automático antiguo {}", old.name());
        });
    }

    private Backup findOrThrow(String name) {
        return storage.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", name));
    }

    private void acquireLock() {
        if (!lock.tryLock()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Hay otra operación de backup en curso, intenta de nuevo en unos minutos");
        }
    }
}