package com.agroruta.worker.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JornalRepository {

    Jornal guardar(Jornal jornal);
    Optional<Jornal> buscarPorId(Long id);

    List<Jornal> buscarPorTrabajador(Long trabajadorId);
    List<Jornal> buscarPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin);

    List<Jornal> buscarPorCultivo(Long cultivoId);
    List<Jornal> buscarPorCultivoYPeriodo(Long cultivoId, LocalDate inicio, LocalDate fin);

    List<Jornal> buscarNoLiquidadosPorTrabajador(Long trabajadorId);
    List<Jornal> buscarNoLiquidadosPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin);

    void eliminar(Long id);
}