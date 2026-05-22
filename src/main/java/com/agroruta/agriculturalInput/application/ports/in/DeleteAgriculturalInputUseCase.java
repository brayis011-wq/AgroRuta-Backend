package com.agroruta.agriculturalInput.application.ports.in;

public interface DeleteAgriculturalInputUseCase {
    /** Soft delete: marca el insumo como inactivo, no lo borra físicamente. */
    void delete(Long id);
}
