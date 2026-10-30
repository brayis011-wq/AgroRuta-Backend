package com.agroruta.backup.domain;

public enum BackupType {
    MANUAL("manual"),
    AUTOMATIC("auto"),
    PRE_RESTORE("prerestore");

    private final String code;

    BackupType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static BackupType fromCode(String code) {
        for (BackupType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de backup desconocido: " + code);
    }
}