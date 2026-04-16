package com.agroruta.cultivo.application;

import com.agroruta.cultivo.application.ports.in.ActividadCultivoUseCase;
import com.agroruta.cultivo.application.ports.in.SiembraUseCase;
import com.agroruta.cultivo.domain.ActividadCultivo;
import com.agroruta.cultivo.domain.ActividadCultivoRepository;
import com.agroruta.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

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
    public ActividadCultivo registrarActividad(ActividadCultivo actividad) {
        // Verificamos que la siembra existe
        siembraUseCase.buscarSiembraPorId(actividad.getSiembraId());

        if (actividad.getDescripcion() == null || actividad.getDescripcion().isBlank()) {
            throw new BusinessException("La descripción de la actividad es obligatoria.");
        }
        if (actividad.getFecha() == null) {
            throw new BusinessException("La fecha de la actividad es obligatoria.");
        }
        return actividadRepository.save(actividad);
    }

    @Override
    public List<ActividadCultivo> listarActividadesPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return actividadRepository.findBySiembraId(siembraId);
    }
}