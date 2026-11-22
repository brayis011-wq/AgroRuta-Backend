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

    // ── Recolectores de una cosecha ──

    /** Quién recogió y cómo se le paga: porKilo = kilos × valorKilo; si no, valorDia (null = el del cargo) */
    record Recoleccion(Long trabajadorId, boolean porKilo, java.math.BigDecimal kilos,
                       java.math.BigDecimal valorKilo, java.math.BigDecimal valorDia) {}

    /**
     * Deja los jornales de la cosecha como vienen en la lista: los que están en una nómina aprobada o pagada
     * deben venir igual (no se pueden cambiar ni quitar); los demás se vuelven a crear.
     */
    List<Jornal> guardarRecolectores(Long cosechaId, Long cultivoId, java.time.LocalDate fecha, List<Recoleccion> recolectores);

    List<Jornal> jornalesDeCosechas(java.util.Collection<Long> cosechaIds);

    /** Estado de la nómina de cada jornal que está en una (los que no, no aparecen) */
    java.util.Map<Long, com.agroruta.worker.domain.EstadoNomina> estadosDeNomina(java.util.Collection<Long> jornalIds);

    /** Al borrar la cosecha: borra sus jornales sin nómina (o en pendiente) y desliga los demás */
    void quitarRecolectores(Long cosechaId);

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

    /** Jornales de todo el equipo (o de un trabajador), por páginas. agricultorId null: todos (Administrador). */
    Pagina<Jornal> paginaDelEquipo(Long agricultorId, Long trabajadorId, LocalDate inicio, LocalDate fin, int pagina, int tamano);

    ResumenJornales resumenDelEquipo(Long agricultorId, Long trabajadorId, LocalDate inicio, LocalDate fin);

    /** Conteos de jornales para el Inicio; hoy null = la fecha de Colombia. */
    com.agroruta.worker.domain.ResumenJornalesInicio resumenInicio(Long agricultorId, LocalDate hoy);
}
