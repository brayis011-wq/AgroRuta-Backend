package com.agroruta.inventory.infrastructure.persistence;

import com.agroruta.inventory.application.ports.out.InventarioRepository;
import com.agroruta.inventory.domain.MovimientoInventario;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InventarioRepositoryImpl implements InventarioRepository {

    private final JpaMovimientoInventarioRepository movimientos;
    private final JpaStockMinimoRepository minimos;

    public InventarioRepositoryImpl(JpaMovimientoInventarioRepository movimientos, JpaStockMinimoRepository minimos) {
        this.movimientos = movimientos;
        this.minimos = minimos;
    }

    @Override
    public MovimientoInventario guardar(MovimientoInventario m) {
        return toDomain(movimientos.save(toEntity(m)));
    }

    @Override
    public Optional<MovimientoInventario> buscarMovimiento(Long id) {
        return movimientos.findById(id).map(InventarioRepositoryImpl::toDomain);
    }

    @Override
    public void eliminarMovimiento(Long id) {
        movimientos.deleteById(id);
    }

    @Override
    public List<MovimientoInventario> movimientosDe(Long agricultorId) {
        return movimientos.findByAgricultorIdOrderByFechaDescIdDesc(agricultorId).stream()
                .map(InventarioRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<MovimientoInventario> movimientosDe(Long agricultorId, Long insumoId) {
        return movimientos.findByAgricultorIdAndInsumoIdOrderByFechaDescIdDesc(agricultorId, insumoId).stream()
                .map(InventarioRepositoryImpl::toDomain).toList();
    }

    @Override
    public Optional<MovimientoInventario> usoDeFumigacion(Long fumigacionId) {
        if (fumigacionId == null) return Optional.empty();
        return movimientos.findByFumigacionId(fumigacionId).map(InventarioRepositoryImpl::toDomain);
    }

    @Override
    public Map<Long, Double> minimosDe(Long agricultorId) {
        return minimos.findByAgricultorId(agricultorId).stream()
                .collect(Collectors.toMap(StockMinimoEntity::getInsumoId, StockMinimoEntity::getMinimo, (a, b) -> b));
    }

    @Override
    public void guardarMinimo(Long agricultorId, Long insumoId, Double minimo) {
        Optional<StockMinimoEntity> actual = minimos.findByAgricultorIdAndInsumoId(agricultorId, insumoId);
        if (minimo == null) {
            actual.ifPresent(minimos::delete);
            return;
        }
        StockMinimoEntity e = actual.orElseGet(StockMinimoEntity::new);
        e.setAgricultorId(agricultorId);
        e.setInsumoId(insumoId);
        e.setMinimo(minimo);
        minimos.save(e);
    }

    static MovimientoInventarioEntity toEntity(MovimientoInventario m) {
        MovimientoInventarioEntity e = new MovimientoInventarioEntity();
        e.setId(m.getId());
        e.setAgricultorId(m.getAgricultorId());
        e.setInsumoId(m.getInsumoId());
        e.setTipo(m.getTipo());
        e.setCantidad(m.getCantidad());
        e.setFecha(m.getFecha());
        e.setCostoTotal(m.getCostoTotal());
        e.setProveedor(m.getProveedor());
        e.setFumigacionId(m.getFumigacionId());
        e.setObservaciones(m.getObservaciones());
        e.setCreadoEn(m.getCreadoEn());
        return e;
    }

    static MovimientoInventario toDomain(MovimientoInventarioEntity e) {
        MovimientoInventario m = new MovimientoInventario();
        m.setId(e.getId());
        m.setAgricultorId(e.getAgricultorId());
        m.setInsumoId(e.getInsumoId());
        m.setTipo(e.getTipo());
        m.setCantidad(e.getCantidad());
        m.setFecha(e.getFecha());
        m.setCostoTotal(e.getCostoTotal());
        m.setProveedor(e.getProveedor());
        m.setFumigacionId(e.getFumigacionId());
        m.setObservaciones(e.getObservaciones());
        m.setCreadoEn(e.getCreadoEn());
        return m;
    }
}
