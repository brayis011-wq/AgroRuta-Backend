package com.agroruta.backup.application;

import com.agroruta.backup.application.ports.out.BackupStoragePort;
import com.agroruta.backup.application.ports.out.DatabaseDumpPort;
import com.agroruta.backup.domain.Backup;
import com.agroruta.backup.domain.BackupType;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest {

    private static final String MANUAL_1 = "agroruta_manual_20200101_020000.sql";

    private InMemoryStorage storage;
    private RecordingDump dump;
    private BackupService service;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
        dump = new RecordingDump(storage);
        service = new BackupService(dump, storage, 2); // retención = 2
    }

    @Test
    void create_manual_dumpsAndReturnsBackup() {
        Backup backup = service.create(BackupType.MANUAL);

        assertEquals(BackupType.MANUAL, backup.type());
        assertTrue(Backup.isValidName(backup.name()));
        assertEquals(1, dump.calls.size());
        assertTrue(dump.calls.get(0).startsWith("dump:agroruta_manual_"));
    }

    @Test
    void create_automatic_appliesRetentionKeepingNewest() {
        storage.seed("agroruta_auto_20200101_020000.sql", Instant.parse("2020-01-01T02:00:00Z"));
        storage.seed("agroruta_auto_20200102_020000.sql", Instant.parse("2020-01-02T02:00:00Z"));
        storage.seed("agroruta_auto_20200103_020000.sql", Instant.parse("2020-01-03T02:00:00Z"));

        Backup nuevo = service.create(BackupType.AUTOMATIC);

        List<String> automaticos = storage.findAll().stream()
                .filter(b -> b.type() == BackupType.AUTOMATIC)
                .map(Backup::name)
                .toList();

        assertEquals(2, automaticos.size());
        assertTrue(automaticos.contains(nuevo.name()));
        assertTrue(automaticos.contains("agroruta_auto_20200103_020000.sql"));
    }

    @Test
    void create_automatic_doesNotTouchManualBackups() {
        storage.seed("agroruta_manual_20200101_020000.sql", Instant.parse("2020-01-01T02:00:00Z"));
        storage.seed("agroruta_manual_20200102_020000.sql", Instant.parse("2020-01-02T02:00:00Z"));
        storage.seed("agroruta_manual_20200103_020000.sql", Instant.parse("2020-01-03T02:00:00Z"));

        service.create(BackupType.AUTOMATIC);

        long manuales = storage.findAll().stream().filter(b -> b.type() == BackupType.MANUAL).count();
        assertEquals(3, manuales);
    }

    @Test
    void restore_createsPreRestoreBackupBeforeRestoring() {
        storage.seed(MANUAL_1, Instant.parse("2020-01-01T02:00:00Z"));

        service.restore(MANUAL_1);

        assertEquals(2, dump.calls.size());
        assertTrue(dump.calls.get(0).startsWith("dump:agroruta_prerestore_"));
        assertEquals("restore:" + MANUAL_1, dump.calls.get(1));
    }

    @Test
    void restore_notFound_throwsAndDoesNothing() {
        assertThrows(ResourceNotFoundException.class, () -> service.restore(MANUAL_1));
        assertTrue(dump.calls.isEmpty());
    }

    @Test
    void delete_removesExistingBackup() {
        storage.seed(MANUAL_1, Instant.parse("2020-01-01T02:00:00Z"));

        service.delete(MANUAL_1);

        assertTrue(storage.findByName(MANUAL_1).isEmpty());
    }

    @Test
    void delete_notFound_throws() {
        assertThrows(ResourceNotFoundException.class, () -> service.delete(MANUAL_1));
    }

    @Test
    void getFile_notFound_throws() {
        assertThrows(ResourceNotFoundException.class, () -> service.getFile(MANUAL_1));
    }

    @Test
    void getFile_returnsPathWhenExists() {
        storage.seed(MANUAL_1, Instant.parse("2020-01-01T02:00:00Z"));
        assertEquals(Path.of(MANUAL_1), service.getFile(MANUAL_1));
    }

    @Test
    void list_returnsStorageContent() {
        storage.seed(MANUAL_1, Instant.parse("2020-01-01T02:00:00Z"));
        assertEquals(1, service.list().size());
    }

    @Test
    void create_whileAnotherOperationRuns_isRejected() throws Exception {
        AtomicReference<Throwable> concurrentError = new AtomicReference<>();

        // Mientras el primer dump está en curso (lock tomado), otro hilo intenta crear un backup
        dump.onDump = () -> {
            Thread other = new Thread(() -> {
                try {
                    service.create(BackupType.MANUAL);
                } catch (Throwable t) {
                    concurrentError.set(t);
                }
            });
            other.start();
            try {
                other.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        service.create(BackupType.MANUAL);

        assertInstanceOf(BusinessException.class, concurrentError.get());
    }

    // ── fakes ────────────────────────────────────────────────────────────────

    private static class InMemoryStorage implements BackupStoragePort {
        private final Map<String, Backup> data = new LinkedHashMap<>();

        void seed(String name, Instant createdAt) {
            data.put(name, new Backup(name, 10L, createdAt, Backup.typeOf(name)));
        }

        @Override public Path pathOf(String name) { return Path.of(name); }
        @Override public Optional<Backup> findByName(String name) { return Optional.ofNullable(data.get(name)); }
        @Override public List<Backup> findAll() {
            List<Backup> all = new ArrayList<>(data.values());
            all.sort(Comparator.comparing(Backup::createdAt).reversed());
            return all;
        }
        @Override public void delete(String name) { data.remove(name); }
    }

    private static class RecordingDump implements DatabaseDumpPort {
        final List<String> calls = new ArrayList<>();
        private final InMemoryStorage storage;
        Runnable onDump;

        RecordingDump(InMemoryStorage storage) { this.storage = storage; }

        @Override public void dump(Path target) {
            String name = target.getFileName().toString();
            calls.add("dump:" + name);
            storage.seed(name, Instant.now());
            if (onDump != null) {
                onDump.run();
            }
        }

        @Override public void restore(Path source) {
            calls.add("restore:" + source.getFileName());
        }
    }
}