package com.agroruta.cultivo.application;

import com.agroruta.cultivo.application.ports.in.FincaUseCase;
import com.agroruta.cultivo.domain.Finca;
import com.agroruta.cultivo.domain.FincaRepository;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FincaService implements FincaUseCase {

    private final FincaRepository fincaRepository;

    public FincaService(FincaRepository fincaRepository) {
        this.fincaRepository = fincaRepository;
    }

    @Override
    public Finca registrarFinca(String nombre, String ubicacion,
                                Double hectareas, Long agricultorId) {
        Finca finca = new Finca(null, nombre, ubicacion, hectareas, agricultorId);
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