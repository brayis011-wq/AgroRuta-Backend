package com.agroruta.crop.application;

import com.agroruta.shared.util.EnumParser;
import com.agroruta.crop.application.ports.in.CosechaUseCase;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.*;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
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
                                    String calidad, String observaciones, Long siembraId, Double precioKg) {

        Siembra siembra = siembraUseCase.buscarSiembraPorId(siembraId);
        if (!siembra.estaActiva()) {
            throw new BusinessException("La siembra está finalizada; reábrela para registrar más cosechas.");
        }

        if (cosechaRepository.existsByFechaAndSiembraId(fecha, siembraId)) {
            throw fechaRepetida(fecha);
        }

        Cosecha cosecha = new Cosecha(
                null, fecha, cantidadKg,
                EnumParser.parse(CalidadCosecha.class, calidad, "calidad"), observaciones, siembraId
        );
        cosecha.setPrecioKg(precioKg);
        return cosechaRepository.save(cosecha);
    }

    @Override
    public List<Cosecha> listarCosechasPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return cosechaRepository.findBySiembraId(siembraId);
    }

    @Override
    public Cosecha buscarCosechaPorId(Long id) {
        return cosechaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cosecha no encontrada con ID: " + id));
    }

    @Override
    public Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                                     String calidad, String observaciones, Double precioKg) {
        Cosecha cosecha = buscarCosechaPorId(id);
        cosecha.setPrecioKg(precioKg);
        return aplicarCambios(cosecha, fecha, cantidadKg, calidad, observaciones);
    }

    @Override
    public Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                                     String calidad, String observaciones) {
        return aplicarCambios(buscarCosechaPorId(id), fecha, cantidadKg, calidad, observaciones);
    }

    private Cosecha aplicarCambios(Cosecha cosecha, LocalDate fecha, Double cantidadKg,
                                   String calidad, String observaciones) {
        if (fecha != null && !fecha.equals(cosecha.getFecha())
                && cosechaRepository.existsByFechaAndSiembraId(fecha, cosecha.getSiembraId())) {
            throw fechaRepetida(fecha);
        }
        if (fecha != null) cosecha.setFecha(fecha);
        if (cantidadKg != null) cosecha.setCantidadKg(cantidadKg);
        if (calidad != null) cosecha.setCalidad(EnumParser.parse(CalidadCosecha.class, calidad, "calidad"));
        cosecha.setObservaciones(observaciones);
        return cosechaRepository.save(cosecha);
    }

    @Override
    public void eliminarCosecha(Long id) {
        buscarCosechaPorId(id);
        cosechaRepository.deleteById(id);
    }

    private static BusinessException fechaRepetida(LocalDate fecha) {
        return new BusinessException(
                ErrorCode.RESOURCE_ALREADY_EXISTS,
                String.format("Ya existe una cosecha registrada el '%s' para esta siembra.", fecha));
    }

    @Override
    public Double totalKgCosechado(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        Double total = cosechaRepository.totalKgBySiembraId(siembraId);
        return total != null ? total : 0.0;
    }
}