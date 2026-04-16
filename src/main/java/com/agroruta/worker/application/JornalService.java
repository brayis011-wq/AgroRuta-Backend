package com.agroruta.worker.application;

import com.agroruta.worker.domain.ActividadRepository;
import com.agroruta.worker.domain.JornalRepository;
import com.agroruta.worker.domain.TrabajadorRepository;
import com.agroruta.worker.domain.Actividad;
import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.domain.Trabajador;

import java.time.LocalDate;
import java.util.List;

public class JornalService {

    private final JornalRepository jornalRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final ActividadRepository actividadRepository;

    public JornalService(JornalRepository jornalRepository,
                         TrabajadorRepository trabajadorRepository,
                         ActividadRepository actividadRepository) {
        this.jornalRepository = jornalRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.actividadRepository = actividadRepository;
    }

    public Jornal registrarJornal(Long trabajadorId, Long cultivoId, String nombreCultivo,
                                  LocalDate fecha, List<Long> actividadIds, String observaciones) {

        Trabajador trabajador = trabajadorRepository.buscarPorId(trabajadorId)
                .orElseThrow(() -> new IllegalArgumentException("Trabajador no encontrado con id: " + trabajadorId));

        if (!trabajador.estaActivo()) {
            throw new IllegalStateException("El trabajador " + trabajador.getNombreCompleto() + " no está activo.");
        }

        Jornal jornal = new Jornal(null, fecha, trabajador, cultivoId, nombreCultivo, observaciones);

        for (Long actividadId : actividadIds) {
            Actividad actividad = actividadRepository.buscarPorId(actividadId)
                    .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada con id: " + actividadId));
            jornal.agregarActividad(actividad);
        }

        return jornalRepository.guardar(jornal);
    }

    public Jornal agregarActividad(Long jornalId, Long actividadId) {
        Jornal jornal = jornalRepository.buscarPorId(jornalId)
                .orElseThrow(() -> new IllegalArgumentException("Jornal no encontrado con id: " + jornalId));

        Actividad actividad = actividadRepository.buscarPorId(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada con id: " + actividadId));

        jornal.agregarActividad(actividad);
        return jornalRepository.guardar(jornal);
    }

    public Jornal removerActividad(Long jornalId, Long actividadId) {
        Jornal jornal = jornalRepository.buscarPorId(jornalId)
                .orElseThrow(() -> new IllegalArgumentException("Jornal no encontrado con id: " + jornalId));

        Actividad actividad = actividadRepository.buscarPorId(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada con id: " + actividadId));

        jornal.removerActividad(actividad);
        return jornalRepository.guardar(jornal);
    }

    public Jornal buscarPorId(Long id) {
        return jornalRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Jornal no encontrado con id: " + id));
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
}