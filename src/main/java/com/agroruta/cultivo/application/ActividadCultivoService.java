package com.agroruta.cultivo.application;

import com.agroruta.cultivo.application.ports.in.ActividadCultivoUseCase;
import com.agroruta.cultivo.application.ports.in.SiembraUseCase;
import com.agroruta.cultivo.domain.ActividadCultivo;
import com.agroruta.cultivo.domain.ActividadCultivoRepository;
import com.agroruta.cultivo.domain.TipoActividad;
import com.agroruta.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

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
                                               LocalDate fecha, Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        if (descripcion == null || descripcion.isBlank())
            throw new BusinessException("La descripción de la actividad es obligatoria.");
        if (fecha == null)
            throw new BusinessException("La fecha de la actividad es obligatoria.");

        ActividadCultivo actividad = new ActividadCultivo(
                null, TipoActividad.valueOf(tipo.toUpperCase()), descripcion, fecha, siembraId
        );
        return actividadRepository.save(actividad);
    }

    @Override
    public List<ActividadCultivo> listarActividadesPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return actividadRepository.findBySiembraId(siembraId);
    }
}