package com.agroruta.inventario.application.ports.in;

import com.agroruta.inventario.domain.MovimientoInventario;
import com.agroruta.inventario.domain.StockInsumo;
import com.agroruta.inventario.domain.UnidadBase;

import java.time.LocalDate;
import java.util.List;

/** Bodega de insumos de un agricultor. Las cantidades que se reciben vienen con su unidad (LITROS, ML, KG, GRAMOS). */
public interface InventarioUseCase {

    /** Insumos con movimientos o con mínimo definido, ordenados por nombre */
    List<StockInsumo> stock(Long agricultorId);

    List<MovimientoInventario> movimientos(Long agricultorId, Long insumoId);

    MovimientoInventario registrarCompra(Long agricultorId, Long insumoId, double cantidad, String unidad,
                                         Double costoTotal, LocalDate fecha, String proveedor, String observaciones);

    /** Conteo físico: deja el stock en {@code cantidadReal} registrando la diferencia como ajuste */
    MovimientoInventario ajustarA(Long agricultorId, Long insumoId, double cantidadReal, String unidad,
                                  String observaciones);

    /** @param minimo null para quitar el aviso */
    StockInsumo definirMinimo(Long agricultorId, Long insumoId, Double minimo, String unidad);

    /** Solo compras y ajustes: el gasto de una fumigación se corrige editando la fumigación */
    void eliminarMovimiento(Long agricultorId, Long movimientoId);

    CostoEstimado costoEstimado(Long agricultorId, Long insumoId, double cantidad, String unidad);

    /**
     * @param costo       lo que costaría la cantidad a precio promedio (null si no hay compras con precio)
     * @param disponible  lo que hay en bodega, en unidad base
     */
    record CostoEstimado(Double costo, Double costoPorUnidad, double disponible, UnidadBase unidadBase) {}
}
