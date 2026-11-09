package com.agroruta.worker.application.ports.out;

import com.agroruta.worker.domain.ResumenJornales;
import com.agroruta.shared.util.Pagina;
import com.agroruta.worker.domain.Jornal;

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

    // ✅ NUEVO: excluye jornales que ya están en alguna nómina
    List<Jornal> buscarDisponiblesParaNomina(Long trabajadorId, LocalDate inicio, LocalDate fin);

    void eliminar(Long id);

    /** Jornales del trabajador, más recientes primero. inicio y fin son opcionales. */
    Pagina<Jornal> paginaPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin, int pagina, int tamano);

    /** Totales de los jornales del trabajador en el mismo rango de fechas. */
    ResumenJornales resumenPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin);
}
