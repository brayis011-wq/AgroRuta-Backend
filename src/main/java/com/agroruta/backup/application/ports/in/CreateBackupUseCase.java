package com.agroruta.backup.application.ports.in;

import com.agroruta.backup.domain.Backup;
import com.agroruta.backup.domain.BackupType;

public interface CreateBackupUseCase {
    Backup create(BackupType type);
}