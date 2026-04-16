package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.Nomina;

import java.time.LocalDate;
import java.util.List;

public interface NominaUseCase {

    Nomina generarNomina(Long trabajadorId, LocalDate periodoInicio, LocalDate periodoFin);

    Nomina aprobarNomina(Long nominaId);

    void anularNomina(Long nominaId);

    Nomina buscarPorId(Long id);

    List<Nomina> listarPorTrabajador(Long trabajadorId);

    List<Nomina> listarPendientesPorTrabajador(Long trabajadorId);

    List<Nomina> listarPorPeriodo(LocalDate inicio, LocalDate fin);

    List<Nomina> listarTodas();
}