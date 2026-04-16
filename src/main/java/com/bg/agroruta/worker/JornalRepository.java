package com.bg.agroruta.worker;

import com.agroRuta.worker.domain.Jornal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JornalRepository {
    Jornal guardar(Jornal jornal);
    Optional<Jornal> buscarPorId(Long id);

    // Consultas por trabajador
    List<Jornal> buscarPorTrabajador(Long trabajadorId);
    List<Jornal> buscarPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin);

    // Consultas por cultivo (vinculación con módulo cultivos)
    List<Jornal> buscarPorCultivo(Long cultivoId);
    List<Jornal> buscarPorCultivoYPeriodo(Long cultivoId, LocalDate inicio, LocalDate fin);

    // Jornales pendientes de liquidar
    List<Jornal> buscarNoLiquidadosPorTrabajador(Long trabajadorId);
    List<Jornal> buscarNoLiquidadosPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin);

    void eliminar(Long id);
}
