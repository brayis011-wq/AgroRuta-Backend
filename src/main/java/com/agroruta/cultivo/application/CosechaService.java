package com.agroruta.cultivo.application;

import com.agroruta.cultivo.application.ports.in.CosechaUseCase;
import com.agroruta.cultivo.application.ports.in.SiembraUseCase;
import com.agroruta.cultivo.domain.Cosecha;
import com.agroruta.cultivo.domain.CosechaRepository;
import com.agroruta.cultivo.domain.EstadoCultivo;
import com.agroruta.cultivo.domain.Siembra;
import com.agroruta.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CosechaService implements CosechaUseCase {

    private final CosechaRepository cosechaRepository;
    private final SiembraUseCase siembraUseCase;

    public CosechaService(CosechaRepository cosechaRepository,
                          SiembraUseCase siembraUseCase) {
        this.cosechaRepository = cosechaRepository;
        this.siembraUseCase = siembraUseCase;
    }

    @Override
    public Cosecha registrarCosecha(Cosecha cosecha) {
        Siembra siembra = siembraUseCase.buscarSiembraPorId(cosecha.getSiembraId());

        // Solo se puede cosechar si el cultivo está en etapa PRODUCCION o COSECHA
        if (siembra.getEstadoCultivo() != EstadoCultivo.PRODUCCION &&
                siembra.getEstadoCultivo() != EstadoCultivo.COSECHA) {
            throw new BusinessException("El cultivo debe estar en etapa PRODUCCION o COSECHA para registrar una cosecha.");
        }

        if (cosecha.getCantidadKg() == null || cosecha.getCantidadKg() <= 0) {
            throw new BusinessException("La cantidad en kg debe ser mayor a cero.");
        }
        if (cosecha.getFecha() == null) {
            throw new BusinessException("La fecha de cosecha es obligatoria.");
        }

        return cosechaRepository.save(cosecha);
    }

    @Override
    public List<Cosecha> listarCosechasPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return cosechaRepository.findBySiembraId(siembraId);
    }

    @Override
    public Double totalKgCosechado(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        Double total = cosechaRepository.totalKgBySiembraId(siembraId);
        return total != null ? total : 0.0;
    }
}