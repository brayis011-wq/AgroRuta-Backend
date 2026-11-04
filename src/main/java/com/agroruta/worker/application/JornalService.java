package com.agroruta.worker.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.out.CultivoPort;
import com.agroruta.worker.application.ports.out.CultivoPort.CultivoInfo;
import com.agroruta.worker.domain.ActividadRepository;
import com.agroruta.worker.domain.JornalRepository;
import com.agroruta.worker.domain.TrabajadorRepository;
import com.agroruta.worker.domain.Actividad;
import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.domain.Trabajador;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import com.agroruta.worker.application.ports.in.JornalUseCase;

@Service
public class JornalService implements JornalUseCase {
    private final JornalRepository jornalRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final ActividadRepository actividadRepository;
    private final CultivoPort cultivoPort;

    public JornalService(JornalRepository jornalRepository,
                         TrabajadorRepository trabajadorRepository,
                         ActividadRepository actividadRepository,
                         CultivoPort cultivoPort) {
        this.jornalRepository = jornalRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.actividadRepository = actividadRepository;
        this.cultivoPort = cultivoPort;
    }

    public Jornal registrarJornal(Long trabajadorId, Long cultivoId, String nombreCultivo,
                                  LocalDate fecha, List<Long> actividadIds, String observaciones) {

        Trabajador trabajador = trabajadorRepository.buscarPorId(trabajadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + trabajadorId));

        if (!trabajador.estaActivo()) {
            throw new IllegalStateException("El trabajador " + trabajador.getNombreCompleto() + " no está activo.");
        }

        // El cultivo debe existir, seguir activo y estar en una finca del mismo agricultor.
        // El nombre se toma del cultivo real (lote · finca), no de lo que envíe el cliente.
        CultivoInfo cultivo = cultivoPort.buscar(cultivoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cultivo no encontrado con id: " + cultivoId));
        if (!Objects.equals(cultivo.agricultorId(), trabajador.getAgricultorId())) {
            throw new BusinessException("El cultivo no pertenece a tus fincas.");
        }
        if (cultivo.finalizado()) {
            throw new BusinessException("El cultivo de " + cultivo.nombre() + " ya está finalizado.");
        }

        Jornal jornal = new Jornal(null, fecha, trabajador, cultivoId, cultivo.nombre(), observaciones);

        for (Long actividadId : actividadIds) {
            Actividad actividad = actividadRepository.buscarPorId(actividadId)
                    .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con id: " + actividadId));
            jornal.agregarActividad(actividad);
        }

        return jornalRepository.guardar(jornal);
    }

    public Jornal agregarActividad(Long jornalId, Long actividadId) {
        Jornal jornal = jornalRepository.buscarPorId(jornalId)
                .orElseThrow(() -> new ResourceNotFoundException("Jornal no encontrado con id: " + jornalId));

        Actividad actividad = actividadRepository.buscarPorId(actividadId)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con id: " + actividadId));

        jornal.agregarActividad(actividad);
        return jornalRepository.guardar(jornal);
    }

    public Jornal removerActividad(Long jornalId, Long actividadId) {
        Jornal jornal = jornalRepository.buscarPorId(jornalId)
                .orElseThrow(() -> new ResourceNotFoundException("Jornal no encontrado con id: " + jornalId));

        Actividad actividad = actividadRepository.buscarPorId(actividadId)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con id: " + actividadId));

        jornal.removerActividad(actividad);
        return jornalRepository.guardar(jornal);
    }

    public Jornal buscarPorId(Long id) {
        return jornalRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jornal no encontrado con id: " + id));
    }

    public List<Jornal> listarPorTrabajador(Long trabajadorId) {
        return jornalRepository.buscarPorTrabajador(trabajadorId);
    }

    public List<Jornal> listarPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return jornalRepository.buscarPorTrabajadorYPeriodo(trabajadorId, inicio, fin);
    }

    public List<Jornal> listarPorCultivo(Long cultivoId) {
        return jornalRepository.buscarPorCultivo(cultivoId);
    }

    public List<Jornal> listarPorCultivoYPeriodo(Long cultivoId, LocalDate inicio, LocalDate fin) {
        return jornalRepository.buscarPorCultivoYPeriodo(cultivoId, inicio, fin);
    }

    public List<CultivoInfo> cultivosDisponibles(Long agricultorId) {
        return cultivoPort.activosDelAgricultor(agricultorId);
    }
}
