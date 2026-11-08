package com.agroruta.worker.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.domain.LaborRepository;
import com.agroruta.worker.domain.Labor;

import java.util.List;

import org.springframework.stereotype.Service;
import com.agroruta.worker.application.ports.in.LaborUseCase;

@Service
public class LaborService implements LaborUseCase {
    private final LaborRepository laborRepository;

    public LaborService(LaborRepository laborRepository) {
        this.laborRepository = laborRepository;
    }

    @Override
    public Labor crearActividad(String nombre, String descripcion) {
        return crearActividad(nombre, descripcion, null);
    }

    @Override
    public Labor crearActividad(String nombre, String descripcion, Long agricultorId) {
        String limpio = limpiar(nombre);
        if (laborRepository.existeNombreVisible(limpio, agricultorId, null)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    String.format("Ya existe una labor con el nombre '%s'.", limpio)
            );
        }

        Labor actividad = new Labor(null, limpio, descripcion);
        actividad.setAgricultorId(agricultorId);
        return laborRepository.guardar(actividad);
    }

    @Override
    public Labor actualizarActividad(Long id, String nombre, String descripcion) {

        Labor actividad = laborRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Labor", id));

        // Valida que el nuevo nombre no lo tenga otra labor que vea el mismo dueño
        String limpio = limpiar(nombre);
        if (laborRepository.existeNombreVisible(limpio, actividad.getAgricultorId(), id)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    String.format("Ya existe otra labor con el nombre '%s'.", limpio)
            );
        }

        actividad.setNombre(limpio);
        actividad.setDescripcion(descripcion);
        return laborRepository.guardar(actividad);
    }

    public void desactivarActividad(Long id) {
        Labor actividad = laborRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Labor no encontrada con id: " + id));
        actividad.desactivar();
        laborRepository.guardar(actividad);
    }

    public Labor buscarPorId(Long id) {
        return laborRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Labor no encontrada con id: " + id));
    }

    public List<Labor> listarActivas() {
        return laborRepository.listarActivas();
    }

    public List<Labor> listarTodas() {
        return laborRepository.listarTodas();
    }

    @Override
    public List<Labor> listarActivasPara(Long agricultorId) {
        return laborRepository.listarActivasPara(agricultorId);
    }

    /** Quita espacios de más: "  Poda   de  formación " → "Poda de formación" */
    static String limpiar(String nombre) {
        String t = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) throw new IllegalArgumentException("El nombre de la labor es obligatorio.");
        return t;
    }
}
