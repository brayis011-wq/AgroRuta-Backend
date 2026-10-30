package com.agroruta.backup.infrastructure.web.dto;

import com.agroruta.backup.domain.Backup;

import java.time.Instant;

public record BackupResponse(String name, long sizeBytes, Instant createdAt, String type) {

    public static BackupResponse from(Backup backup) {
        return new BackupResponse(
                backup.name(),
                backup.sizeBytes(),
                backup.createdAt(),
                backup.type().name());
    }
}