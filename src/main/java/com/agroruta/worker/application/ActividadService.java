package com.agroruta.worker.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
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

    @Override
    public Actividad crearActividad(String nombre, String descripcion) {

        if (actividadRepository.existsByNombre(nombre)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    String.format("Ya existe una actividad con el nombre '%s'.", nombre)
            );
        }

        Actividad actividad = new Actividad(null, nombre, descripcion);
        return actividadRepository.guardar(actividad);
    }

    @Override
    public Actividad actualizarActividad(Long id, String nombre, String descripcion) {

        Actividad actividad = actividadRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad", id));

        // Valida que el nuevo nombre no lo tenga otra actividad distinta
        if (actividadRepository.existsByNombreAndIdNot(nombre, id)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    String.format("Ya existe otra actividad con el nombre '%s'.", nombre)
            );
        }

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
