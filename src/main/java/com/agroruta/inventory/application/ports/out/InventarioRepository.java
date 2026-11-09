package com.agroruta.inventory.application.ports.out;

import com.agroruta.inventory.domain.MovimientoInventario;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InventarioRepository {

    MovimientoInventario guardar(MovimientoInventario movimiento);

    Optional<MovimientoInventario> buscarMovimiento(Long id);

    void eliminarMovimiento(Long id);

    /** Todos los movimientos de la bodega del agricultor, del más reciente al más antiguo */
    List<MovimientoInventario> movimientosDe(Long agricultorId);

    List<MovimientoInventario> movimientosDe(Long agricultorId, Long insumoId);

    /** El gasto de producto registrado por una fumigación, si hay */
    Optional<MovimientoInventario> usoDeFumigacion(Long fumigacionId);

    /** Mínimos definidos por el agricultor: insumoId → cantidad en unidad base */
    Map<Long, Double> minimosDe(Long agricultorId);

    /** @param minimo null para quitarlo */
    void guardarMinimo(Long agricultorId, Long insumoId, Double minimo);
}
