package com.agroruta.cultivo.application;

import com.agroruta.cultivo.application.ports.in.FumigacionUseCase;
import com.agroruta.cultivo.application.ports.in.SiembraUseCase;
import com.agroruta.cultivo.domain.Fumigacion;
import com.agroruta.cultivo.domain.FumigacionRepository;
import com.agroruta.cultivo.domain.UnidadMedida;
import com.agroruta.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    public Fumigacion registrarFumigacion(LocalDate fecha, String producto, Double dosis,
                                          String unidadMedida, Double areaAplicada,
                                          String observaciones, Long siembraId) {
        Fumigacion fumigacion = new Fumigacion(
                null, fecha, producto, dosis,
                UnidadMedida.valueOf(unidadMedida.toUpperCase()),
                areaAplicada, observaciones, siembraId
        );
        return fumigacionRepository.save(fumigacion);
    }

    @Override
    public List<Fumigacion> listarFumigacionesPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return fumigacionRepository.findBySiembraId(siembraId);
    }
}