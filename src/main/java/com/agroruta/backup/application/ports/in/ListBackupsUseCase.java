package com.agroruta.backup.application.ports.in;

import com.agroruta.backup.domain.Backup;

import java.util.List;

public interface ListBackupsUseCase {
    List<Backup> list();
}