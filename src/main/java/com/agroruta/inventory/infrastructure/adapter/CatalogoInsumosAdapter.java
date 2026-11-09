package com.agroruta.inventory.infrastructure.adapter;

import com.agroruta.agriculturalinput.application.ports.out.AgriculturalInputRepositoryPort;
import com.agroruta.inventory.application.ports.out.CatalogoInsumosPort;
import com.agroruta.inventory.domain.InsumoCatalogo;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Lee el catálogo de insumos, incluidos los desactivados (pueden seguir teniendo stock o historial). */
@Component
public class CatalogoInsumosAdapter implements CatalogoInsumosPort {

    private final AgriculturalInputRepositoryPort insumos;

    public CatalogoInsumosAdapter(AgriculturalInputRepositoryPort insumos) {
        this.insumos = insumos;
    }

    @Override
    public Optional<InsumoCatalogo> buscar(Long insumoId) {
        if (insumoId == null) return Optional.empty();
        return insumos.findAnyById(insumoId).map(i -> new InsumoCatalogo(
                i.getId(), i.getNombre(), i.getTipo() == null ? null : i.getTipo().name(),
                i.getUnidadSugerida(), i.isActivo()));
    }
}
