package com.agroruta.cultivo.application;

import com.agroruta.cultivo.application.ports.in.FumigacionUseCase;
import com.agroruta.cultivo.application.ports.in.SiembraUseCase;
import com.agroruta.cultivo.domain.Fumigacion;
import com.agroruta.cultivo.domain.FumigacionRepository;
import com.agroruta.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FumigacionService implements FumigacionUseCase {

    private final FumigacionRepository fumigacionRepository;
    private final SiembraUseCase siembraUseCase;

    public FumigacionService(FumigacionRepository fumigacionRepository,
                             SiembraUseCase siembraUseCase) {
        this.fumigacionRepository = fumigacionRepository;
        this.siembraUseCase = siembraUseCase;
    }

    @Override
    public Fumigacion registrarFumigacion(Fumigacion fumigacion) {
        // Verificamos que la siembra existe
        siembraUseCase.buscarSiembraPorId(fumigacion.getSiembraId());

        if (fumigacion.getProducto() == null || fumigacion.getProducto().isBlank()) {
            throw new BusinessException("El producto de la fumigación es obligatorio.");
        }
        if (fumigacion.getDosis() == null || fumigacion.getDosis() <= 0) {
            throw new BusinessException("La dosis debe ser mayor a cero.");
        }
        if (fumigacion.getFecha() == null) {
            throw new BusinessException("La fecha de fumigación es obligatoria.");
        }
        return fumigacionRepository.save(fumigacion);
    }

    @Override
    public List<Fumigacion> listarFumigacionesPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return fumigacionRepository.findBySiembraId(siembraId);
    }
}