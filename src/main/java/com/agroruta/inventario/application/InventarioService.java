package com.agroruta.inventario.application;

import com.agroruta.agriculturalInput.domain.MeasurementUnit;
import com.agroruta.crop.application.ports.out.InventarioPort;
import com.agroruta.inventario.application.ports.in.InventarioUseCase;
import com.agroruta.inventario.application.ports.in.StockBajoUseCase;
import com.agroruta.inventario.application.ports.out.CatalogoInsumosPort;
import com.agroruta.inventario.domain.*;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.util.EnumParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bodega de insumos: una por agricultor. El stock no se guarda, se calcula sumando los movimientos
 * (compras, gastos en fumigaciones y ajustes), así nunca queda desfasado de su historial.
 */
@Service
public class InventarioService implements InventarioUseCase, StockBajoUseCase, InventarioPort {

    private final InventarioRepository repositorio;
    private final CatalogoInsumosPort catalogo;
    private final Clock reloj;

    @Autowired
    public InventarioService(InventarioRepository repositorio, CatalogoInsumosPort catalogo) {
        this(repositorio, catalogo, Clock.system(ZoneId.of("America/Bogota")));
    }

    InventarioService(InventarioRepository repositorio, CatalogoInsumosPort catalogo, Clock reloj) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    // ── Consultas ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<StockInsumo> stock(Long agricultorId) {
        Map<Long, List<MovimientoInventario>> porInsumo = repositorio.movimientosDe(agricultorId).stream()
                .collect(Collectors.groupingBy(MovimientoInventario::getInsumoId));
        Map<Long, Double> minimos = repositorio.minimosDe(agricultorId);
        Set<Long> insumos = new TreeSet<>(porInsumo.keySet());
        insumos.addAll(minimos.keySet());

        List<StockInsumo> lista = new ArrayList<>();
        for (Long id : insumos) {
            catalogo.buscar(id).ifPresent(insumo -> lista.add(CalculadoraStock.calcular(
                    insumo, porInsumo.getOrDefault(id, List.of()), minimos.get(id))));
        }
        lista.sort(Comparator.comparing(StockInsumo::nombre, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockInsumo> avisos(Long agricultorId) {
        return stock(agricultorId).stream().filter(s -> s.estado().requiereAtencion()).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventario> movimientos(Long agricultorId, Long insumoId) {
        insumo(insumoId);
        return repositorio.movimientosDe(agricultorId, insumoId);
    }

    @Override
    @Transactional(readOnly = true)
    public CostoEstimado costoEstimado(Long agricultorId, Long insumoId, double cantidad, String unidad) {
        InsumoCatalogo insumo = insumo(insumoId);
        double base = UnidadBase.aBaseDe(cantidad, unidad(unidad), insumo.unidadBase(), insumo.nombre());
        List<MovimientoInventario> movimientos = repositorio.movimientosDe(agricultorId, insumoId);
        Double promedio = CalculadoraStock.costoPromedio(movimientos);
        return new CostoEstimado(promedio == null ? null : pesos(base * promedio), promedio,
                CalculadoraStock.cantidad(movimientos), insumo.unidadBase());
    }

    // ── Compras, ajustes y mínimos ────────────────────────────────────

    @Override
    @Transactional
    public MovimientoInventario registrarCompra(Long agricultorId, Long insumoId, double cantidad, String unidad,
                                                Double costoTotal, LocalDate fecha, String proveedor, String observaciones) {
        InsumoCatalogo insumo = insumo(insumoId);
        if (cantidad <= 0) throw new BusinessException("La cantidad comprada debe ser mayor a 0.");
        if (costoTotal != null && costoTotal < 0) throw new BusinessException("El costo no puede ser negativo.");
        LocalDate dia = fecha != null ? fecha : hoy();
        if (dia.isAfter(hoy())) throw new BusinessException("La fecha de compra no puede ser futura.");
        double base = UnidadBase.aBaseDe(cantidad, unidad(unidad), insumo.unidadBase(), insumo.nombre());
        return repositorio.guardar(MovimientoInventario.compra(
                agricultorId, insumoId, base, costoTotal, dia, proveedor, observaciones));
    }

    @Override
    @Transactional
    public MovimientoInventario ajustarA(Long agricultorId, Long insumoId, double cantidadReal, String unidad,
                                         String observaciones) {
        InsumoCatalogo insumo = insumo(insumoId);
        if (cantidadReal < 0) throw new BusinessException("La cantidad contada no puede ser negativa.");
        double real = UnidadBase.aBaseDe(cantidadReal, unidad(unidad), insumo.unidadBase(), insumo.nombre());
        double diferencia = real - CalculadoraStock.cantidad(repositorio.movimientosDe(agricultorId, insumoId));
        if (Math.abs(diferencia) < 0.001) {
            throw new BusinessException("El stock ya es " + cantidadReal + ": no hay nada que ajustar.");
        }
        return repositorio.guardar(MovimientoInventario.ajuste(agricultorId, insumoId, diferencia, hoy(), observaciones));
    }

    @Override
    @Transactional
    public StockInsumo definirMinimo(Long agricultorId, Long insumoId, Double minimo, String unidad) {
        InsumoCatalogo insumo = insumo(insumoId);
        Double base = null;
        if (minimo != null) {
            if (minimo < 0) throw new BusinessException("El mínimo no puede ser negativo.");
            base = UnidadBase.aBaseDe(minimo, unidad(unidad), insumo.unidadBase(), insumo.nombre());
        }
        repositorio.guardarMinimo(agricultorId, insumoId, base);
        return CalculadoraStock.calcular(insumo, repositorio.movimientosDe(agricultorId, insumoId), base);
    }

    @Override
    @Transactional
    public void eliminarMovimiento(Long agricultorId, Long movimientoId) {
        MovimientoInventario m = repositorio.buscarMovimiento(movimientoId)
                .filter(x -> Objects.equals(x.getAgricultorId(), agricultorId))
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento no encontrado con ID: " + movimientoId));
        if (m.getTipo() == TipoMovimiento.USO) {
            throw new BusinessException("Este gasto viene de una fumigación: corrígelo editando o eliminando la fumigación.");
        }
        repositorio.eliminarMovimiento(movimientoId);
    }

    // ── Gasto de las fumigaciones (InventarioPort) ───────────────────

    @Override
    @Transactional
    public Optional<Double> registrarUso(Long fumigacionId, Long agricultorId, Long insumoId,
                                         double cantidad, String unidad, LocalDate fecha) {
        InsumoCatalogo insumo = insumo(insumoId);
        if (cantidad <= 0) throw new BusinessException("La cantidad usada debe ser mayor a 0.");
        double base = UnidadBase.aBaseDe(cantidad, unidad(unidad), insumo.unidadBase(), insumo.nombre());
        MovimientoInventario uso = repositorio.usoDeFumigacion(fumigacionId)
                .orElseGet(() -> MovimientoInventario.uso(agricultorId, insumoId, base, fecha, fumigacionId));
        uso.setAgricultorId(agricultorId);
        uso.setInsumoId(insumoId);
        uso.setCantidad(base);
        uso.setFecha(fecha != null ? fecha : hoy());
        repositorio.guardar(uso);
        Double promedio = CalculadoraStock.costoPromedio(repositorio.movimientosDe(agricultorId, insumoId));
        return Optional.ofNullable(promedio).map(p -> pesos(base * p));
    }

    @Override
    @Transactional
    public void quitarUso(Long fumigacionId) {
        repositorio.usoDeFumigacion(fumigacionId).ifPresent(m -> repositorio.eliminarMovimiento(m.getId()));
    }

    // ── apoyo ─────────────────────────────────────────────────────────

    private InsumoCatalogo insumo(Long insumoId) {
        if (insumoId == null) throw new BusinessException("El insumo es obligatorio.");
        return catalogo.buscar(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado con ID: " + insumoId));
    }

    private static MeasurementUnit unidad(String unidad) {
        return EnumParser.parse(MeasurementUnit.class, unidad, "unidad de medida");
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private static double pesos(double v) {
        return Math.round(v);
    }
}
