package com.agroruta.inventario.application.ports.out;

import com.agroruta.inventario.domain.InsumoCatalogo;

import java.util.Optional;

/** Consulta al catálogo de insumos (módulo agriculturalInput), también los desactivados. */
public interface CatalogoInsumosPort {
    Optional<InsumoCatalogo> buscar(Long insumoId);
}
