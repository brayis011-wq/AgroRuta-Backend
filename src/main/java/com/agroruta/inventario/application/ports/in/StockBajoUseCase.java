package com.agroruta.inventario.application.ports.in;

import com.agroruta.inventario.domain.StockInsumo;

import java.util.List;

/** Insumos que necesitan atención (bajos, agotados o en negativo), para el inicio y el correo diario. */
public interface StockBajoUseCase {
    List<StockInsumo> avisos(Long agricultorId);
}
