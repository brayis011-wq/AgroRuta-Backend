package com.agroruta.backup.infrastructure.storage;

import com.agroruta.backup.application.ports.out.BackupStoragePort;
import com.agroruta.backup.infrastructure.config.BackupProperties;
import com.agroruta.backup.domain.Backup;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class FileSystemBackupStorage implements BackupStoragePort {

    private final Path root;

    public FileSystemBackupStorage(BackupProperties properties) {
        this.root = Path.of(properties.getDirectory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de backups: " + root, e);
        }
    }

    @Override
    public Path pathOf(String name) {
        if (!Backup.isValidName(name)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Nombre de backup inválido");
        }
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Nombre de backup inválido");
        }
        return path;
    }

    @Override
    public Optional<Backup> findByName(String name) {
        Path path = pathOf(name);
        return Files.isRegularFile(path) ? toBackup(path) : Optional.empty();
    }

    @Override
    public List<Backup> findAll() {
        try (Stream<Path> files = Files.list(root)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(p -> Backup.isValidName(p.getFileName().toString()))
                    .map(this::toBackup)
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(Backup::createdAt).reversed())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo listar la carpeta de backups", e);
        }
    }

    @Override
    public void delete(String name) {
        try {
            Files.deleteIfExists(pathOf(name));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar el backup " + name, e);
        }
    }

    private Optional<Backup> toBackup(Path path) {
        try {
            String name = path.getFileName().toString();
            return Optional.of(new Backup(
                    name,
                    Files.size(path),
                    Files.getLastModifiedTime(path).toInstant(),
                    Backup.typeOf(name)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}