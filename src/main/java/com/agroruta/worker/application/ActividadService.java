package com.agroruta.worker.application;

import com.agroruta.worker.domain.ActividadRepository;
import com.agroruta.worker.domain.Actividad;

import java.util.List;

import org.springframework.stereotype.Service;
import com.agroruta.worker.application.ports.in.ActividadUseCase;

@Service
public class ActividadService implements ActividadUseCase {
    private final ActividadRepository actividadRepository;

    public ActividadService(ActividadRepository actividadRepository) {
        this.actividadRepository = actividadRepository;
    }

    public Actividad crearActividad(String nombre, String descripcion) {
        Actividad actividad = new Actividad(null, nombre, descripcion);
        return actividadRepository.guardar(actividad);
    }

    public Actividad actualizarActividad(Long id, String nombre, String descripcion) {
        Actividad actividad = actividadRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada con id: " + id));
        actividad.setNombre(nombre);
        actividad.setDescripcion(descripcion);
        return actividadRepository.guardar(actividad);
    }

    public void desactivarActividad(Long id) {
        Actividad actividad = actividadRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada con id: " + id));
        actividad.desactivar();
        actividadRepository.guardar(actividad);
    }

    public Actividad buscarPorId(Long id) {
        return actividadRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada con id: " + id));
    }

    public List<Actividad> listarActivas() {
        return actividadRepository.listarActivas();
    }

    public List<Actividad> listarTodas() {
        return actividadRepository.listarTodas();
    }
}
