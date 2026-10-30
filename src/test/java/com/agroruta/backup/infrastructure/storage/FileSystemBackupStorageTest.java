package com.agroruta.backup.infrastructure.storage;

import com.agroruta.backup.config.BackupProperties;
import com.agroruta.backup.domain.Backup;
import com.agroruta.backup.domain.BackupType;
import com.agroruta.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemBackupStorageTest {

    @TempDir
    Path tempDir;

    private FileSystemBackupStorage storage;

    @BeforeEach
    void setUp() {
        BackupProperties props = new BackupProperties();
        props.setDirectory(tempDir.toString());
        storage = new FileSystemBackupStorage(props);
    }

    @Test
    void pathOf_rejectsPathTraversal() {
        assertThrows(BusinessException.class, () -> storage.pathOf("../../etc/passwd"));
        assertThrows(BusinessException.class, () -> storage.pathOf("..\\secret.sql"));
    }

    @Test
    void pathOf_rejectsNamesOutsideNamingConvention() {
        assertThrows(BusinessException.class, () -> storage.pathOf("cualquier.sql"));
        assertThrows(BusinessException.class, () -> storage.pathOf(null));
    }

    @Test
    void pathOf_acceptsValidNameInsideRoot() {
        Path path = storage.pathOf("agroruta_manual_20260101_020000.sql");
        assertTrue(path.startsWith(tempDir.toAbsolutePath().normalize()));
    }

    @Test
    void findAll_listsOnlyValidBackupsNewestFirst() throws IOException {
        create("agroruta_manual_20260101_020000.sql", "2026-01-01T02:00:00Z");
        create("agroruta_auto_20260102_020000.sql", "2026-01-02T02:00:00Z");
        Files.writeString(tempDir.resolve("notas.txt"), "ignorar");

        List<Backup> all = storage.findAll();

        assertEquals(2, all.size());
        assertEquals("agroruta_auto_20260102_020000.sql", all.get(0).name());
        assertEquals(BackupType.AUTOMATIC, all.get(0).type());
        assertEquals(BackupType.MANUAL, all.get(1).type());
    }

    @Test
    void findByName_returnsEmptyWhenMissing() {
        assertTrue(storage.findByName("agroruta_manual_20260101_020000.sql").isEmpty());
    }

    @Test
    void delete_removesFile() throws IOException {
        create("agroruta_manual_20260101_020000.sql", "2026-01-01T02:00:00Z");

        storage.delete("agroruta_manual_20260101_020000.sql");

        assertTrue(storage.findByName("agroruta_manual_20260101_020000.sql").isEmpty());
    }

    private void create(String name, String modified) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, "-- dump");
        Files.setLastModifiedTime(file, FileTime.from(Instant.parse(modified)));
    }
}