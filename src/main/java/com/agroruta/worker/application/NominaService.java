package com.agroruta.worker.application;

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
import com.agroruta.worker.application.ports.in.NominaUseCase;
@Service
public class NominaService implements NominaUseCase{

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

    public Nomina aprobarNomina(Long nominaId) {
        Nomina nomina = nominaRepository.buscarPorId(nominaId)
                .orElseThrow(() -> new IllegalArgumentException("Nómina no encontrada con id: " + nominaId));

        nomina.marcarComoPagada();

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