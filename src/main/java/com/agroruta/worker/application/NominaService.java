package com.agroruta.worker.application;

import com.agroRuta.worker.application.ports.JornalRepository;
import com.agroRuta.worker.application.ports.NominaRepository;
import com.agroRuta.worker.application.ports.TrabajadorRepository;
import com.agroRuta.worker.domain.EstadoNomina;
import com.agroRuta.worker.domain.Jornal;
import com.agroRuta.worker.domain.Nomina;
import com.agroRuta.worker.domain.Trabajador;

import java.time.LocalDate;
import java.util.List;

public class NominaService {

    private final NominaRepository nominaRepository;
    private final JornalRepository jornalRepository;
    private final TrabajadorRepository trabajadorRepository;

    public NominaService(NominaRepository nominaRepository,
                         JornalRepository jornalRepository,
                         TrabajadorRepository trabajadorRepository) {
        this.nominaRepository = nominaRepository;
        this.jornalRepository = jornalRepository;
        this.trabajadorRepository = trabajadorRepository;
    }

    /**
     * Genera la nómina de un trabajador para un período dado.
     * Toma todos los jornales NO liquidados dentro del rango de fechas
     * y calcula el total automáticamente.
     */
    public Nomina generarNomina(Long trabajadorId, LocalDate periodoInicio, LocalDate periodoFin) {
        Trabajador trabajador = trabajadorRepository.buscarPorId(trabajadorId)
                .orElseThrow(() -> new IllegalArgumentException("Trabajador no encontrado con id: " + trabajadorId));

        List<Jornal> jornalesPendientes = jornalRepository
                .buscarNoLiquidadosPorTrabajadorYPeriodo(trabajadorId, periodoInicio, periodoFin);

        if (jornalesPendientes.isEmpty()) {
            throw new IllegalStateException(
                    "No hay jornales pendientes de liquidar para el trabajador "
                    + trabajador.getNombreCompleto()
                    + " en el período indicado."
            );
        }

        Nomina nomina = new Nomina(null, trabajador, periodoInicio, periodoFin, jornalesPendientes);
        return nominaRepository.guardar(nomina);
    }

    /**
     * Marca la nómina como pagada y liquida todos sus jornales.
     * Este paso es previo al registro del Pago.
     */
    public Nomina aprobarNomina(Long nominaId) {
        Nomina nomina = nominaRepository.buscarPorId(nominaId)
                .orElseThrow(() -> new IllegalArgumentException("Nómina no encontrada con id: " + nominaId));

        nomina.marcarComoPagada();

        // Persistir jornales actualizados (marcados como liquidados)
        nomina.getJornales().forEach(jornalRepository::guardar);

        return nominaRepository.guardar(nomina);
    }

    public void anularNomina(Long nominaId) {
        Nomina nomina = nominaRepository.buscarPorId(nominaId)
                .orElseThrow(() -> new IllegalArgumentException("Nómina no encontrada con id: " + nominaId));
        nomina.anular();
        nominaRepository.guardar(nomina);
    }

    public Nomina buscarPorId(Long id) {
        return nominaRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Nómina no encontrada con id: " + id));
    }

    public List<Nomina> listarPorTrabajador(Long trabajadorId) {
        return nominaRepository.buscarPorTrabajador(trabajadorId);
    }

    public List<Nomina> listarPendientesPorTrabajador(Long trabajadorId) {
        return nominaRepository.buscarPorTrabajadorYEstado(trabajadorId, EstadoNomina.PENDIENTE);
    }

    public List<Nomina> listarPorPeriodo(LocalDate inicio, LocalDate fin) {
        return nominaRepository.buscarPorPeriodo(inicio, fin);
    }

    public List<Nomina> listarTodas() {
        return nominaRepository.listarTodas();
    }
}
