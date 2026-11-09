package com.agroruta.inventory.infrastructure.web;

import com.agroruta.inventory.application.ports.in.InventarioUseCase;
import com.agroruta.inventory.application.ports.in.StockBajoUseCase;
import com.agroruta.inventory.domain.MovimientoInventario;
import com.agroruta.inventory.domain.StockInsumo;
import com.agroruta.inventory.infrastructure.web.dto.AjusteRequest;
import com.agroruta.inventory.infrastructure.web.dto.CompraRequest;
import com.agroruta.inventory.infrastructure.web.dto.MinimoRequest;
import com.agroruta.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bodega de insumos del usuario autenticado. Todo se filtra por su id: no hay forma de ver o
 * tocar la bodega de otro agricultor.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventarioController {

    private final InventarioUseCase inventario;
    private final StockBajoUseCase stockBajo;

    public InventarioController(InventarioUseCase inventario, StockBajoUseCase stockBajo) {
        this.inventario = inventario;
        this.stockBajo = stockBajo;
    }

    @GetMapping
    public ResponseEntity<List<StockInsumo>> stock(Authentication auth) {
        return ResponseEntity.ok(inventario.stock(CurrentUser.id(auth)));
    }

    /** Insumos bajos, agotados o en negativo (para el inicio) */
    @GetMapping("/alerts")
    public ResponseEntity<List<StockInsumo>> avisos(Authentication auth) {
        return ResponseEntity.ok(stockBajo.avisos(CurrentUser.id(auth)));
    }

    @GetMapping("/{insumoId}/movements")
    public ResponseEntity<List<MovimientoInventario>> movimientos(@PathVariable Long insumoId, Authentication auth) {
        return ResponseEntity.ok(inventario.movimientos(CurrentUser.id(auth), insumoId));
    }

    @PostMapping("/purchases")
    public ResponseEntity<MovimientoInventario> compra(@Valid @RequestBody CompraRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventario.registrarCompra(
                CurrentUser.id(auth), req.getInsumoId(), req.getCantidad(), req.getUnidad(), req.getCostoTotal(),
                req.getFecha(), req.getProveedor(), req.getObservaciones()));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<MovimientoInventario> ajuste(@Valid @RequestBody AjusteRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventario.ajustarA(
                CurrentUser.id(auth), req.getInsumoId(), req.getCantidadReal(), req.getUnidad(), req.getObservaciones()));
    }

    @PutMapping("/{insumoId}/minimum")
    public ResponseEntity<StockInsumo> minimo(@PathVariable Long insumoId, @Valid @RequestBody MinimoRequest req,
                                              Authentication auth) {
        return ResponseEntity.ok(inventario.definirMinimo(CurrentUser.id(auth), insumoId, req.getMinimo(), req.getUnidad()));
    }

    @DeleteMapping("/movements/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        inventario.eliminarMovimiento(CurrentUser.id(auth), id);
        return ResponseEntity.noContent().build();
    }

    /** Lo que costaría una cantidad a precio promedio de compra (para la fumigación) */
    @GetMapping("/estimated-cost")
    public ResponseEntity<InventarioUseCase.CostoEstimado> costoEstimado(@RequestParam Long insumoId,
                                                                        @RequestParam double cantidad,
                                                                        @RequestParam String unidad,
                                                                        Authentication auth) {
        return ResponseEntity.ok(inventario.costoEstimado(CurrentUser.id(auth), insumoId, cantidad, unidad));
    }
}
