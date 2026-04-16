package com.agroruta.cultivo.application;

import com.agroruta.cultivo.application.ports.in.FincaUseCase;
import com.agroruta.cultivo.application.ports.in.LoteUseCase;
import com.agroruta.cultivo.domain.Lote;
import com.agroruta.cultivo.domain.LoteRepository;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoteService implements LoteUseCase {

    private final LoteRepository loteRepository;
    private final FincaUseCase fincaUseCase;

    public LoteService(LoteRepository loteRepository, FincaUseCase fincaUseCase) {
        this.loteRepository = loteRepository;
        this.fincaUseCase = fincaUseCase;
    }

    @Override
    public Lote registrarLote(Lote lote) {
        fincaUseCase.buscarFincaPorId(lote.getFincaId());
        if (lote.getNombre() == null || lote.getNombre().isBlank()) {
            throw new BusinessException("El nombre del lote es obligatorio.");
        }
        return loteRepository.save(lote);
    }

    @Override
    public Lote buscarLotePorId(Long id) {
        return loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + id));
    }

    @Override
    public List<Lote> listarLotesPorFinca(Long fincaId) {
        fincaUseCase.buscarFincaPorId(fincaId);
        return loteRepository.findByFincaId(fincaId);
    }
}