package com.bg.agroruta.cultivo.application;

import com.bg.agroruta.cultivo.application.ports.in.FincaUseCase;
import com.bg.agroruta.cultivo.domain.Finca;
import com.bg.agroruta.cultivo.domain.FincaRepository;
import com.bg.agroruta.shared.exception.BusinessException;
import com.bg.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FincaService implements FincaUseCase {

    private final FincaRepository fincaRepository;

    public FincaService(FincaRepository fincaRepository) {
        this.fincaRepository = fincaRepository;
    }

    @Override
    public Finca registrarFinca(Finca finca) {
        if (finca.getNombre() == null || finca.getNombre().isBlank()) {
            throw new BusinessException("El nombre de la finca es obligatorio.");
        }
        return fincaRepository.save(finca);
    }

    @Override
    public Finca buscarFincaPorId(Long id) {
        return fincaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Finca no encontrada con ID: " + id));
    }

    @Override
    public List<Finca> listarFincasPorAgricultor(Long agricultorId) {
        return fincaRepository.findByAgricultorId(agricultorId);
    }
}