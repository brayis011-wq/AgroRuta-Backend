package com.agroruta.inventory.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una entrada o salida de un insumo en la bodega de un agricultor.
 * La cantidad está en la unidad base del insumo (ml o g) y siempre es positiva, salvo en los
 * ajustes, donde el signo dice si se sumó o se restó.
 */
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario {

    private Long id;
    private Long agricultorId;
    private Long insumoId;
    private TipoMovimiento tipo;
    private double cantidad;
    private LocalDate fecha;
    /** Lo que costó la compra completa (COP); null en usos y ajustes */
    private Double costoTotal;
    private String proveedor;
    /** Fumigación que gastó el producto (solo en USO) */
    private Long fumigacionId;
    private String observaciones;
    private LocalDateTime creadoEn;

    public static MovimientoInventario compra(Long agricultorId, Long insumoId, double cantidadBase,
                                              Double costoTotal, LocalDate fecha, String proveedor, String observaciones) {
        MovimientoInventario m = base(agricultorId, insumoId, TipoMovimiento.COMPRA, cantidadBase, fecha);
        m.costoTotal = costoTotal;
        m.proveedor = vacioANull(proveedor);
        m.observaciones = vacioANull(observaciones);
        return m;
    }

    public static MovimientoInventario uso(Long agricultorId, Long insumoId, double cantidadBase,
                                           LocalDate fecha, Long fumigacionId) {
        MovimientoInventario m = base(agricultorId, insumoId, TipoMovimiento.USO, cantidadBase, fecha);
        m.fumigacionId = fumigacionId;
        return m;
    }

    /** @param diferencia positiva si sobraba producto, negativa si faltaba */
    public static MovimientoInventario ajuste(Long agricultorId, Long insumoId, double diferencia,
                                              LocalDate fecha, String observaciones) {
        MovimientoInventario m = base(agricultorId, insumoId, TipoMovimiento.AJUSTE, diferencia, fecha);
        m.observaciones = vacioANull(observaciones);
        return m;
    }

    /** Cuánto suma (o resta) al stock */
    public double efecto() {
        return switch (tipo) {
            case COMPRA, AJUSTE -> cantidad;
            case USO -> -cantidad;
        };
    }

    private static MovimientoInventario base(Long agricultorId, Long insumoId, TipoMovimiento tipo,
                                             double cantidad, LocalDate fecha) {
        MovimientoInventario m = new MovimientoInventario();
        m.agricultorId = agricultorId;
        m.insumoId = insumoId;
        m.tipo = tipo;
        m.cantidad = cantidad;
        m.fecha = fecha;
        m.creadoEn = LocalDateTime.now();
        return m;
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
