package com.agroruta.backup.application.ports.out;

import com.agroruta.backup.domain.Backup;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface BackupStoragePort {

    /** Ruta validada de un backup (no verifica que exista). Lanza BusinessException si el nombre es inválido. */
    Path pathOf(String name);

    Optional<Backup> findByName(String name);

    /** Todos los backups, del más reciente al más antiguo. */
    List<Backup> findAll();

    void delete(String name);
}