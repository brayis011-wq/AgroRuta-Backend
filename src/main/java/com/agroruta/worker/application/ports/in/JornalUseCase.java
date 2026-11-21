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

    /** @param kilos solo si el cargo del trabajador se paga por kilo (entonces es obligatorio) */
    Jornal registrarJornal(Long trabajadorId, Long cultivoId, String nombreCultivo, LocalDate fecha,
                           List<Long> actividadIds, String observaciones, java.math.BigDecimal kilos);

    /** La cuadrícula de jornales del equipo entre dos fechas (máximo 16 días). agricultorId null: todos. */
    com.agroruta.worker.domain.PlanillaSemanal planilla(Long agricultorId, LocalDate inicio, LocalDate fin);

    /** Varios jornales a la vez desde la planilla; no repite el mismo trabajador, día y cultivo. */
    List<Jornal> registrarVarios(Long agricultorId, Long cultivoId, List<Long> actividadIds,
                                 List<com.agroruta.worker.domain.PlanillaSemanal.Entrada> entradas);

    /** Quita un jornal que no está en ninguna nómina. */
    void eliminarJornal(Long jornalId);

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

    /** Conteos de jornales para el Inicio; hoy null = la fecha de Colombia. */
    com.agroruta.worker.domain.ResumenJornalesInicio resumenInicio(Long agricultorId, LocalDate hoy);
}
