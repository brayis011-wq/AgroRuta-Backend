package com.agroruta.agriculturalInput.domain.exception;

public class AgriculturalInputNotFoundException extends RuntimeException {

    public AgriculturalInputNotFoundException(Long id) {
        super("Insumo agrícola no encontrado con id: " + id);
    }
}
