package com.agroruta.finance.application.ports.out;

import com.agroruta.finance.domain.GastoFinca;
import com.agroruta.finance.domain.ResumenGastos;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GastosFincaRepository {

    GastoFinca crear(GastoFinca gasto);

    GastoFinca actualizar(GastoFinca gasto);

    void eliminar(Long id);

    Optional<GastoFinca> buscarPorId(Long id);

    /**
     * Gastos de las fincas del agricultor (o de una sola), más recientes primero.
     * agricultorId null: los de todos (Administrador). desde y hasta son opcionales.
     */
    List<GastoFinca> listar(Long agricultorId, Long fincaId, LocalDate desde, LocalDate hasta);

    ResumenGastos resumen(Long agricultorId, LocalDate desde, LocalDate hasta);
}
