package com.agroruta.finance.application.ports.in;

import com.agroruta.finance.domain.ResumenVentas;
import com.agroruta.finance.domain.Venta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Ventas y cuentas por cobrar: a quién le vendiste, cuánto y cuánto te debe todavía. */
public interface VentasUseCase {

    record DatosVenta(Long siembraId, String comprador, LocalDate fecha, String producto, BigDecimal cantidadKg,
                      BigDecimal precioKg, BigDecimal total, LocalDate vence, String observaciones) {}

    /** Con pagadoDeContado, queda un abono por el total el mismo día */
    Venta registrar(Long agricultorId, DatosVenta datos, boolean pagadoDeContado, String metodo);

    Venta actualizar(Long id, DatosVenta datos);

    void eliminar(Long id);

    Venta buscarPorId(Long id);

    /** estado null: todas */
    List<Venta> listar(Long agricultorId, Venta.Estado estado, String comprador, LocalDate desde, LocalDate hasta);

    ResumenVentas resumen(Long agricultorId);

    Venta abonar(Long ventaId, LocalDate fecha, BigDecimal monto, String metodo, String observaciones);

    Venta quitarAbono(Long ventaId, Long abonoId);

    List<String> compradores(Long agricultorId);

    /** Kilos cosechados y vendidos por siembra: lo que queda para vender */
    List<com.agroruta.finance.domain.KilosEnBodega> kilosEnBodega(Long agricultorId);
}
