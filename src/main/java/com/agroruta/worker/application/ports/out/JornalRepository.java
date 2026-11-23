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

    /** Kilos y pagos por trabajador entre dos fechas (solo jornales con kilos). agricultorId null: todos */
    List<com.agroruta.worker.domain.RendimientoTrabajador> rendimiento(Long agricultorId, LocalDate inicio, LocalDate fin);

    /** Los jornales de los recolectores de esas cosechas */
    List<Jornal> buscarPorCosechas(java.util.Collection<Long> cosechaIds);

    /** Jornales de todos los trabajadores del agricultor entre dos fechas; agricultorId null: los de todos */
    List<Jornal> buscarDelEquipo(Long agricultorId, LocalDate inicio, LocalDate fin);

    /** De los jornales dados, los que ya están en alguna nómina */
    java.util.Set<Long> idsEnNomina(java.util.Collection<Long> ids);

    /** De los jornales dados que están en una nómina, el estado de esa nómina */
    java.util.Map<Long, com.agroruta.worker.domain.EstadoNomina> estadosDeNomina(java.util.Collection<Long> ids);

    /** Jornales del trabajador, más recientes primero. inicio y fin son opcionales. */
    Pagina<Jornal> paginaPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin, int pagina, int tamano);

    /** Jornales del equipo del agricultor (o de un trabajador si viene), más recientes primero. agricultorId null: todos. */
    Pagina<Jornal> paginaDelEquipo(Long agricultorId, Long trabajadorId, LocalDate inicio, LocalDate fin, int pagina, int tamano);

    /** Totales con los mismos filtros */
    ResumenJornales resumenDelEquipo(Long agricultorId, Long trabajadorId, LocalDate inicio, LocalDate fin);

    /** Totales de los jornales del trabajador en el mismo rango de fechas. */
    ResumenJornales resumenPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin);

    /**
     * Jornales de este mes (hasta hoy) y del mes anterior, y quiénes tienen jornal hoy, de los trabajadores
     * activos del agricultor. Con agricultorId null (Administrador) cuenta los de todos.
     */
    com.agroruta.worker.domain.ResumenJornalesInicio resumenInicio(Long agricultorId, LocalDate hoy);
}
