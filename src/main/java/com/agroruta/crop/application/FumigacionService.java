package com.agroruta.crop.application;

import com.agroruta.shared.util.EnumParser;
import com.agroruta.crop.application.ports.in.FumigacionUseCase;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.Fumigacion;
import com.agroruta.crop.domain.RestriccionesFumigacion;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
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
                                          Long siembraId,
                                          RestriccionesFumigacion restricciones) {

        Siembra siembra = siembraUseCase.buscarSiembraPorId(siembraId);
        if (!siembra.estaActiva()) {
            throw new BusinessException("La siembra está finalizada; reábrela para registrar más fumigaciones.");
        }

        UnidadMedida unidad = EnumParser.parse(UnidadMedida.class, unidadMedida, "unidad de medida");

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

        fumigacion.fijarRestricciones(restricciones);
        return fumigacionRepository.save(fumigacion);
    }

    @Override
    public Fumigacion buscarFumigacionPorId(Long id) {
        return fumigacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fumigación no encontrada con ID: " + id));
    }

    @Override
    public Fumigacion actualizarFumigacion(Long id, LocalDate fecha, String producto,
                                           Long agriculturalInputId, Double dosis, String unidadMedida,
                                           Double areaAplicada, String observaciones,
                                           RestriccionesFumigacion restricciones) {
        Fumigacion fumigacion = buscarFumigacionPorId(id);
        if (fecha != null) fumigacion.setFecha(fecha);
        if (producto != null && !producto.isBlank()) {
            fumigacion.setProducto(producto.trim());
            // si se escribió a mano, deja de apuntar al catálogo
            fumigacion.setAgriculturalInputId(agriculturalInputId);
        }
        if (dosis != null) fumigacion.setDosis(dosis);
        if (unidadMedida != null) {
            fumigacion.setUnidadMedida(EnumParser.parse(UnidadMedida.class, unidadMedida, "unidad de medida"));
        }
        if (areaAplicada != null) fumigacion.setAreaAplicada(areaAplicada);
        fumigacion.setObservaciones(observaciones);
        // null = no se enviaron (se conservan las guardadas)
        if (restricciones != null) fumigacion.fijarRestricciones(restricciones);
        return fumigacionRepository.save(fumigacion);
    }

    @Override
    public void eliminarFumigacion(Long id) {
        buscarFumigacionPorId(id);
        fumigacionRepository.deleteById(id);
    }

    @Override
    public List<Fumigacion> listarFumigacionesPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return fumigacionRepository.findBySiembraId(siembraId);
    }
}