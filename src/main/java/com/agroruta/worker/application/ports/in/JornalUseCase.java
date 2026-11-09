package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.ResumenJornales;
import com.agroruta.shared.util.Pagina;
import com.agroruta.worker.application.ports.out.CultivoPort;
import com.agroruta.worker.domain.Jornal;

import java.time.LocalDate;
import java.util.List;

public interface JornalUseCase {

    Jornal registrarJornal(Long trabajadorId, Long cultivoId, String nombreCultivo,
                           LocalDate fecha, List<Long> actividadIds, String observaciones);

    Jornal agregarActividad(Long jornalId, Long actividadId);

    Jornal removerActividad(Long jornalId, Long actividadId);

    Jornal buscarPorId(Long id);

    List<Jornal> listarPorTrabajador(Long trabajadorId);

    List<Jornal> listarPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin);

    List<Jornal> listarPorCultivo(Long cultivoId);

    List<Jornal> listarPorCultivoYPeriodo(Long cultivoId, LocalDate inicio, LocalDate fin);

    /** Cultivos activos del agricultor, para elegir en qué cultivo se trabajó. */
    List<CultivoPort.CultivoInfo> cultivosDisponibles(Long agricultorId);

    Pagina<Jornal> paginaPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin, int pagina, int tamano);
    ResumenJornales resumenPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin);
}
