package com.agroruta.backup.infrastructure.web;

import com.agroruta.backup.application.ports.in.CreateBackupUseCase;
import com.agroruta.backup.application.ports.in.DeleteBackupUseCase;
import com.agroruta.backup.application.ports.in.DownloadBackupUseCase;
import com.agroruta.backup.application.ports.in.ListBackupsUseCase;
import com.agroruta.backup.application.ports.in.RestoreBackupUseCase;
import com.agroruta.backup.domain.BackupType;
import com.agroruta.backup.infrastructure.web.dto.BackupResponse;
import com.agroruta.backup.infrastructure.web.dto.RestoreRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;

/** Acceso restringido a Administrador en SecurityConfig (/api/backups/**). */
@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
public class BackupController {

    private final CreateBackupUseCase createBackup;
    private final ListBackupsUseCase listBackups;
    private final DownloadBackupUseCase downloadBackup;
    private final DeleteBackupUseCase deleteBackup;
    private final RestoreBackupUseCase restoreBackup;

    @PostMapping
    public ResponseEntity<BackupResponse> crear() {
        BackupResponse body = BackupResponse.from(createBackup.create(BackupType.MANUAL));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public ResponseEntity<List<BackupResponse>> listar() {
        return ResponseEntity.ok(listBackups.list().stream().map(BackupResponse::from).toList());
    }

    @GetMapping("/{name:.+}/download")
    public ResponseEntity<Resource> descargar(@PathVariable String name) {
        Path file = downloadBackup.getFile(name);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }

    @PostMapping("/{name:.+}/restore")
    public ResponseEntity<Void> restaurar(@PathVariable String name,
                                          @Valid @RequestBody RestoreRequest request) {
        restoreBackup.restore(name);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{name:.+}")
    public ResponseEntity<Void> eliminar(@PathVariable String name) {
        deleteBackup.delete(name);
        return ResponseEntity.noContent().build();
    }
}