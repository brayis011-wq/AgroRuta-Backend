package com.agroruta.agriculturalInput.domain;

public enum AgriculturalInputType {

    FUNGICIDA("Fungicida"),
    INSECTICIDA("Insecticida"),
    ACARICIDA("Acaricida"),
    NEMATICIDA("Nematicida"),
    HERBICIDA("Herbicida"),
    FERTILIZANTE_FOLIAR("Fertilizante foliar"),
    FERTILIZANTE_SUELO("Fertilizante de suelo"),
    REGULADOR_CRECIMIENTO("Regulador de crecimiento"),
    COADYUVANTE("Coadyuvante");

    private final String displayName;

    AgriculturalInputType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}