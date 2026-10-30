package com.agroruta.backup.application.ports.in;

import java.nio.file.Path;

public interface DownloadBackupUseCase {
    Path getFile(String name);
}