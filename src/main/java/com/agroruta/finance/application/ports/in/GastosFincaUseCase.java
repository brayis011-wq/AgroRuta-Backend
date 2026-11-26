package com.agroruta.finance.application.ports.in;

import com.agroruta.finance.domain.GastoFinca;
import com.agroruta.finance.domain.ResumenGastos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Gastos generales de la finca (los que no son de una siembra). */
public interface GastosFincaUseCase {

    GastoFinca registrar(
            Long fincaId, LocalDate fecha, GastoFinca.Categoria categoria, String descripcion, BigDecimal monto);

    GastoFinca actualizar(
            Long id,
            Long fincaId,
            LocalDate fecha,
            GastoFinca.Categoria categoria,
            String descripcion,
            BigDecimal monto);

    void eliminar(Long id);

    List<GastoFinca> listar(Long agricultorId, Long fincaId, LocalDate desde, LocalDate hasta);

    ResumenGastos resumen(Long agricultorId, LocalDate desde, LocalDate hasta);
}
