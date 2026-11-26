package com.agroruta.finance.application.ports.out;

import com.agroruta.finance.domain.Venta;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VentasRepository {

    Venta crear(Long agricultorId, Venta venta);

    void actualizar(Venta venta);

    void eliminar(Long id);

    /** Con sus abonos */
    Optional<Venta> buscarPorId(Long id);

    /** Ventas del agricultor (sin el detalle de abonos), más recientes primero. agricultorId null: todas. */
    List<Venta> listar(Long agricultorId, LocalDate desde, LocalDate hasta);

    Venta.Abono agregarAbono(Venta.Abono abono);

    void eliminarAbono(Long ventaId, Long abonoId);

    /** Lo cobrado (suma de abonos) entre dos fechas */
    java.math.BigDecimal cobradoEntre(Long agricultorId, LocalDate desde, LocalDate hasta);

    /** Nombres de compradores ya usados, para sugerirlos */
    List<String> compradores(Long agricultorId);

    /** Ventas de una siembra (sin abonos), para los ingresos de la temporada */
    List<Venta> listarPorSiembra(Long siembraId);

    /** Por siembra con cosechas: kilos recogidos y vendidos. agricultorId null: todas */
    List<com.agroruta.finance.domain.KilosEnBodega> kilosEnBodega(Long agricultorId);

    /** La bodega de una siembra; vacío si no tiene cosechas */
    java.util.Optional<com.agroruta.finance.domain.KilosEnBodega> kilosDeSiembra(Long siembraId);
}
