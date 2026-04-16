package com.agroruta.cultivo.application;

import com.agroruta.cultivo.application.ports.in.SiembraUseCase;
import com.agroruta.cultivo.application.ports.in.LoteUseCase;
import com.agroruta.cultivo.domain.*;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SiembraService implements SiembraUseCase {

    private final SiembraRepository siembraRepository;
    private final LoteUseCase loteUseCase;

    public SiembraService(SiembraRepository siembraRepository, LoteUseCase loteUseCase) {
        this.siembraRepository = siembraRepository;
        this.loteUseCase = loteUseCase;
    }

    @Override
    public Siembra registrarSiembra(LocalDate fechaSiembra, Integer cantidadPlantas,
                                    String variedad, Long loteId) {
        Siembra siembra = new Siembra(
                null, fechaSiembra, cantidadPlantas,
                VariedadUchuva.valueOf(variedad.toUpperCase()), loteId
        );
        return siembraRepository.save(siembra);
    }

    @Override
    public Siembra buscarSiembraPorId(Long id) {
        return siembraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Siembra no encontrada con ID: " + id));
    }

    @Override
    public Siembra buscarSiembraPorLote(Long loteId) {
        return siembraRepository.findByLoteId(loteId)
                .orElseThrow(() -> new ResourceNotFoundException("No hay siembra activa en el lote: " + loteId));
    }

    @Override
    public Siembra avanzarEtapa(Long siembraId) {
        Siembra siembra = buscarSiembraPorId(siembraId);
        siembra.avanzarEtapa();
        return siembraRepository.save(siembra);
    }

    @Override
    public List<Siembra> listarSiembrasPorEstado(String estado) {
        EstadoCultivo estadoCultivo = EstadoCultivo.valueOf(estado.toUpperCase());
        return siembraRepository.findByEstadoCultivo(estadoCultivo);
    }
}