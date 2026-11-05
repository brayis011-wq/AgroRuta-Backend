package com.agroruta.crop.application;

import com.agroruta.shared.util.EnumParser;
import com.agroruta.crop.application.ports.in.ActividadCultivoUseCase;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.ActividadCultivo;
import com.agroruta.crop.domain.ActividadCultivoRepository;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.crop.domain.TipoActividad;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import com.agroruta.shared.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;

@Service
public class ActividadCultivoService implements ActividadCultivoUseCase {

    private final ActividadCultivoRepository actividadRepository;
    private final SiembraUseCase siembraUseCase;

    public ActividadCultivoService(ActividadCultivoRepository actividadRepository,
                                   SiembraUseCase siembraUseCase) {
        this.actividadRepository = actividadRepository;
        this.siembraUseCase = siembraUseCase;
    }

    @Override
    public ActividadCultivo registrarActividad(String tipo, String descripcion,
                                               LocalDate fecha, Long siembraId, Double costo) {
        Siembra siembra = siembraUseCase.buscarSiembraPorId(siembraId);
        if (!siembra.estaActiva()) {
            throw new BusinessException("La siembra está finalizada; reábrela para registrar más actividades.");
        }

        if (descripcion == null || descripcion.isBlank())
            throw new BusinessException("La descripción de la actividad es obligatoria.");
        if (fecha == null)
            throw new BusinessException("La fecha de la actividad es obligatoria.");

        TipoActividad tipoActividad = EnumParser.parse(TipoActividad.class, tipo, "tipo de actividad");

        if (actividadRepository.existsByTipoAndFechaAndSiembraId(tipoActividad, fecha, siembraId)) {
            throw repetida(tipo, fecha);
        }

        ActividadCultivo actividad = new ActividadCultivo(
                null, tipoActividad, descripcion, fecha, siembraId
        );
        actividad.setCosto(costo);
        return actividadRepository.save(actividad);
    }

    @Override
    public ActividadCultivo buscarActividadPorId(Long id) {
        return actividadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con ID: " + id));
    }

    @Override
    public ActividadCultivo actualizarActividad(Long id, String tipo, String descripcion, LocalDate fecha, Double costo) {
        ActividadCultivo actividad = buscarActividadPorId(id);
        actividad.setCosto(costo);
        return actualizar(actividad, tipo, descripcion, fecha);
    }

    @Override
    public ActividadCultivo actualizarActividad(Long id, String tipo, String descripcion, LocalDate fecha) {
        return actualizar(buscarActividadPorId(id), tipo, descripcion, fecha);
    }

    private ActividadCultivo actualizar(ActividadCultivo actividad, String tipo, String descripcion, LocalDate fecha) {
        if (descripcion == null || descripcion.isBlank())
            throw new BusinessException("La descripción de la actividad es obligatoria.");

        TipoActividad nuevoTipo = tipo != null
                ? EnumParser.parse(TipoActividad.class, tipo, "tipo de actividad")
                : actividad.getTipo();
        LocalDate nuevaFecha = fecha != null ? fecha : actividad.getFecha();
        boolean cambia = nuevoTipo != actividad.getTipo() || !nuevaFecha.equals(actividad.getFecha());
        if (cambia && actividadRepository.existsByTipoAndFechaAndSiembraId(
                nuevoTipo, nuevaFecha, actividad.getSiembraId())) {
            throw repetida(nuevoTipo.name(), nuevaFecha);
        }
        actividad.setTipo(nuevoTipo);
        actividad.setFecha(nuevaFecha);
        actividad.setDescripcion(descripcion.trim());
        return actividadRepository.save(actividad);
    }

    @Override
    public void eliminarActividad(Long id) {
        buscarActividadPorId(id);
        actividadRepository.deleteById(id);
    }

    private static BusinessException repetida(String tipo, LocalDate fecha) {
        return new BusinessException(
                ErrorCode.RESOURCE_ALREADY_EXISTS,
                String.format("Ya existe una actividad de tipo '%s' registrada el '%s' para esta siembra.", tipo, fecha));
    }

    @Override
    public List<ActividadCultivo> listarActividadesPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return actividadRepository.findBySiembraId(siembraId);
    }
}