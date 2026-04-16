package com.agroruta.cultivo.application;

import com.agroruta.cultivo.application.ports.in.CosechaUseCase;
import com.agroruta.cultivo.application.ports.in.SiembraUseCase;
import com.agroruta.cultivo.domain.*;
import com.agroruta.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    public Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg,
                                    String calidad, String observaciones, Long siembraId) {
        Cosecha cosecha = new Cosecha(
                null, fecha, cantidadKg,
                CalidadCosecha.valueOf(calidad.toUpperCase()), observaciones, siembraId
        );
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