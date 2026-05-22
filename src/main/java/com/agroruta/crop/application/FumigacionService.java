package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.FumigacionUseCase;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.Fumigacion;
import com.agroruta.crop.domain.FumigacionRepository;
import com.agroruta.crop.domain.UnidadMedida;
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
    public Fumigacion registrarFumigacion(LocalDate fecha,
                                          String producto,
                                          Long agriculturalInputId,   // ← nuevo parámetro
                                          Double dosis,
                                          String unidadMedida,
                                          Double areaAplicada,
                                          String observaciones,
                                          Long siembraId) {

        siembraUseCase.buscarSiembraPorId(siembraId);

        UnidadMedida unidad = UnidadMedida.valueOf(unidadMedida.toUpperCase());

        /*
         * Se elige el factory method según si el usuario seleccionó un insumo
         * del catálogo o lo escribió manualmente.
         * En ambos casos `producto` (String) siempre se persiste para
         * mantener el historial aunque el insumo sea eliminado del catálogo.
         */
        Fumigacion fumigacion = (agriculturalInputId != null)
                ? Fumigacion.fromCatalog(fecha, producto, agriculturalInputId,
                dosis, unidad, areaAplicada, observaciones, siembraId)
                : Fumigacion.fromManualEntry(fecha, producto,
                dosis, unidad, areaAplicada, observaciones, siembraId);

        return fumigacionRepository.save(fumigacion);
    }

    @Override
    public List<Fumigacion> listarFumigacionesPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return fumigacionRepository.findBySiembraId(siembraId);
    }
}