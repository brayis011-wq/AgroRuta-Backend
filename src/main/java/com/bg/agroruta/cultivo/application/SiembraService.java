package com.bg.agroruta.cultivo.application;

import com.bg.agroruta.cultivo.application.ports.in.SiembraUseCase;
import com.bg.agroruta.cultivo.application.ports.in.LoteUseCase;
import com.bg.agroruta.cultivo.domain.EstadoCultivo;
import com.bg.agroruta.cultivo.domain.Lote;
import com.bg.agroruta.cultivo.domain.Siembra;
import com.bg.agroruta.cultivo.domain.SiembraRepository;
import com.bg.agroruta.shared.exception.BusinessException;
import com.bg.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

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
    public Siembra registrarSiembra(Siembra siembra) {
        // Verificamos que el lote existe
        Lote lote = loteUseCase.buscarLotePorId(siembra.getLoteId());

        // Verificamos que el lote no tenga ya una siembra activa
        if (siembraRepository.findByLoteId(siembra.getLoteId()).isPresent()) {
            throw new BusinessException("El lote ya tiene una siembra activa.");
        }

        // Cambiamos el estado del lote a EN_CULTIVO
        lote.iniciarCultivo();

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