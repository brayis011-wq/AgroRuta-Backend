package com.bg.agroruta.worker;

import com.agroRuta.worker.domain.EstadoNomina;
import com.agroRuta.worker.domain.Nomina;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NominaRepository {
    Nomina guardar(Nomina nomina);
    Optional<Nomina> buscarPorId(Long id);
    List<Nomina> buscarPorTrabajador(Long trabajadorId);
    List<Nomina> buscarPorTrabajadorYEstado(Long trabajadorId, EstadoNomina estado);
    List<Nomina> buscarPorPeriodo(LocalDate inicio, LocalDate fin);
    List<Nomina> listarTodas();
    void eliminar(Long id);
}
