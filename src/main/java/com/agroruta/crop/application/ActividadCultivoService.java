package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.ActividadCultivoUseCase;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.ActividadCultivo;
import com.agroruta.crop.domain.ActividadCultivoRepository;
import com.agroruta.crop.domain.TipoActividad;
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
                                               LocalDate fecha, Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);

        if (descripcion == null || descripcion.isBlank())
            throw new BusinessException("La descripción de la actividad es obligatoria.");
        if (fecha == null)
            throw new BusinessException("La fecha de la actividad es obligatoria.");

        TipoActividad tipoActividad = TipoActividad.valueOf(tipo.toUpperCase());

        if (actividadRepository.existsByTipoAndFechaAndSiembraId(tipoActividad, fecha, siembraId)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    String.format("Ya existe una actividad de tipo '%s' registrada el '%s' para esta siembra.", tipo, fecha)
            );
        }

        ActividadCultivo actividad = new ActividadCultivo(
                null, tipoActividad, descripcion, fecha, siembraId
        );
        return actividadRepository.save(actividad);
    }

    @Override
    public List<ActividadCultivo> listarActividadesPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return actividadRepository.findBySiembraId(siembraId);
    }
}