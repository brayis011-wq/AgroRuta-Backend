package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.ResumenNominas;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.shared.util.Pagina;
import com.agroruta.worker.domain.Nomina;

import java.time.LocalDate;
import java.util.List;

public interface NominaUseCase {
    Nomina generarNomina(Long trabajadorId, LocalDate periodoInicio, LocalDate periodoFin);

    /**
     * La quincena de todo el equipo en un paso: una nómina por cada trabajador que tenga jornales sin
     * nómina en el periodo. Devuelve las creadas; quien no tiene jornales se salta.
     */
    List<Nomina> generarParaTodos(Long agricultorId, LocalDate periodoInicio, LocalDate periodoFin);

    /** Aprueba las nóminas pendientes de la lista (las demás se dejan como están). */
    List<Nomina> aprobarVarias(Long agricultorId, List<Long> nominaIds);
    Nomina aprobarNomina(Long nominaId);
    void anularNomina(Long nominaId);
    void eliminarNomina(Long nominaId);
    Nomina reactivarNomina(Long nominaId); // ✅ NUEVO
    Nomina buscarPorId(Long id);
    List<Nomina> listarPorTrabajador(Long trabajadorId);
    List<Nomina> listarPendientesPorTrabajador(Long trabajadorId);
    List<Nomina> listarPorPeriodo(LocalDate inicio, LocalDate fin);
    List<Nomina> listarTodas();
    List<Nomina> listarTodasDe(Long agricultorId);

    Pagina<Nomina> pagina(Long agricultorId, Long trabajadorId, EstadoNomina estado, int pagina, int tamano);
    List<ResumenNominas> resumenPorEstado(Long agricultorId, Long trabajadorId);
}
