package com.agroruta.worker.application;

import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.domain.JornalRepository;
import com.agroruta.worker.domain.NominaRepository;
import com.agroruta.worker.domain.TrabajadorRepository;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Trabajador;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.agroruta.worker.application.ports.in.NominaUseCase;

@Service
@Transactional
public class NominaService implements NominaUseCase {

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

    @Override
    public Nomina generarNomina(Long trabajadorId, LocalDate periodoInicio, LocalDate periodoFin) {
        Trabajador trabajador = trabajadorRepository.buscarPorId(trabajadorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trabajador no encontrado con id: " + trabajadorId));

        List<Jornal> jornalesDisponibles = jornalRepository
                .buscarDisponiblesParaNomina(trabajadorId, periodoInicio, periodoFin);

        if (jornalesDisponibles.isEmpty()) {
            throw new IllegalStateException(
                    "No hay jornales disponibles para el trabajador "
                            + trabajador.getNombreCompleto()
                            + " en el período indicado.");
        }

        Nomina nomina = new Nomina(null, trabajador, periodoInicio, periodoFin, jornalesDisponibles);
        return nominaRepository.guardar(nomina);
    }

    /**
     * Solo aprueba: PENDIENTE → APROBADA.
     * La nómina pasa a PAGADA cuando se registra el pago (PagoService.registrarPago).
     */
    @Override
    public Nomina aprobarNomina(Long nominaId) {
        Nomina nomina = nominaRepository.buscarPorId(nominaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nómina no encontrada con id: " + nominaId));
        nomina.aprobar();
        return nominaRepository.guardar(nomina);
    }

    @Override
    public void anularNomina(Long nominaId) {
        Nomina nomina = nominaRepository.buscarPorId(nominaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nómina no encontrada con id: " + nominaId));
        nomina.anular();
        nominaRepository.guardar(nomina);
    }

    @Override
    public Nomina reactivarNomina(Long nominaId) {
        Nomina nomina = nominaRepository.buscarPorId(nominaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nómina no encontrada con id: " + nominaId));

        if (!EstadoNomina.ANULADA.equals(nomina.getEstado())) {
            throw new IllegalStateException(
                    "Solo se pueden reactivar nóminas en estado ANULADA.");
        }

        nomina.reactivar();
        return nominaRepository.guardar(nomina);
    }

    @Override
    public void eliminarNomina(Long nominaId) {
        Nomina nomina = nominaRepository.buscarPorId(nominaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nómina no encontrada con id: " + nominaId));

        if (!EstadoNomina.PENDIENTE.equals(nomina.getEstado())) {
            throw new IllegalStateException(
                    "Solo se pueden eliminar nóminas en estado PENDIENTE.");
        }

        nominaRepository.eliminar(nominaId);
    }

    @Override
    public Nomina buscarPorId(Long id) {
        return nominaRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nómina no encontrada con id: " + id));
    }

    @Override
    public List<Nomina> listarPorTrabajador(Long trabajadorId) {
        return nominaRepository.buscarPorTrabajador(trabajadorId);
    }

    @Override
    public List<Nomina> listarPendientesPorTrabajador(Long trabajadorId) {
        return nominaRepository.buscarPorTrabajadorYEstado(trabajadorId, EstadoNomina.PENDIENTE);
    }

    @Override
    public List<Nomina> listarPorPeriodo(LocalDate inicio, LocalDate fin) {
        return nominaRepository.buscarPorPeriodo(inicio, fin);
    }

    @Override
    public List<Nomina> listarTodas() {
        return nominaRepository.listarTodas();
    }
    @Override
    public List<Nomina> listarTodasDe(Long agricultorId) {
        if (agricultorId == null) return List.of();
        return nominaRepository.listarPorAgricultor(agricultorId);
    }
}