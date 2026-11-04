package com.agroruta.crop.application;

import com.agroruta.shared.util.EnumParser;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.application.ports.in.LoteUseCase;
import com.agroruta.crop.domain.*;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;

@Service
public class SiembraService implements SiembraUseCase {

    private final SiembraRepository siembraRepository;
    private final LoteUseCase loteUseCase;
    private final CatalogoCultivoRepository catalogo;

    public SiembraService(SiembraRepository siembraRepository, LoteUseCase loteUseCase,
                          CatalogoCultivoRepository catalogo) {
        this.siembraRepository = siembraRepository;
        this.loteUseCase = loteUseCase;
        this.catalogo = catalogo;
    }

    @Override
    public Siembra registrarSiembra(LocalDate fechaSiembra, Integer cantidadPlantas,
                                    Long variedadId, Long loteId) {

        loteUseCase.buscarLotePorId(loteId);

        if (siembraRepository.existsByLoteId(loteId)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "El lote ya tiene una siembra registrada. Un lote solo puede tener una siembra activa."
            );
        }

        VariedadCultivo variedad = catalogo.buscarVariedad(variedadId)
                .orElseThrow(() -> new ResourceNotFoundException("Variedad no encontrada con ID: " + variedadId));
        if (!variedad.isActivo() || !variedad.isTipoCultivoActivo()) {
            throw new BusinessException(
                    "La variedad " + variedad.getNombre() + " de " + variedad.getTipoCultivoNombre()
                            + " ya no está disponible para nuevas siembras.");
        }

        Siembra siembra = new Siembra(null, fechaSiembra, cantidadPlantas, variedad, loteId);
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
        EstadoCultivo estadoCultivo = EnumParser.parse(EstadoCultivo.class, estado, "estado");
        return siembraRepository.findByEstadoCultivo(estadoCultivo);
    }

    @Override
    public void eliminarSiembra(Long id) {
        buscarSiembraPorId(id); // valida que exista
        siembraRepository.deleteById(id);
    }

}