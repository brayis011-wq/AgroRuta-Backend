package com.agroruta.agriculturalInput.domain.exception;

import com.agroruta.shared.exception.ResourceNotFoundException;

/** Hereda de ResourceNotFoundException para que el GlobalExceptionHandler responda 404. */
public class AgriculturalInputNotFoundException extends ResourceNotFoundException {

    public AgriculturalInputNotFoundException(Long id) {
        super("Insumo agrícola no encontrado con id: " + id);
    }
}
