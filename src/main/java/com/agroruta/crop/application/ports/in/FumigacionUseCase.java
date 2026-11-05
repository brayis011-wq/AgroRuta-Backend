package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.Fumigacion;
import com.agroruta.crop.domain.RestriccionesFumigacion;
import java.time.LocalDate;
import java.util.List;

public interface FumigacionUseCase {

    /**
     * @param agriculturalInputId ID del catálogo si el usuario seleccionó un insumo,
     *                            null si lo escribió manualmente.
     */
    Fumigacion registrarFumigacion(LocalDate fecha,
                                   String producto,
                                   Long agriculturalInputId,
                                   Double dosis,
                                   String unidadMedida,
                                   Double areaAplicada,
                                   String observaciones,
                                   Long siembraId,
                                   RestriccionesFumigacion restricciones);

    /** Sin días de carencia ni horas de reentrada */
    default Fumigacion registrarFumigacion(LocalDate fecha, String producto, Long agriculturalInputId,
                                           Double dosis, String unidadMedida, Double areaAplicada,
                                           String observaciones, Long siembraId) {
        return registrarFumigacion(fecha, producto, agriculturalInputId, dosis, unidadMedida,
                areaAplicada, observaciones, siembraId, RestriccionesFumigacion.NINGUNA);
    }

    List<Fumigacion> listarFumigacionesPorSiembra(Long siembraId);

    Fumigacion buscarFumigacionPorId(Long id);

    Fumigacion actualizarFumigacion(Long id,
                                    LocalDate fecha,
                                    String producto,
                                    Long agriculturalInputId,
                                    Double dosis,
                                    String unidadMedida,
                                    Double areaAplicada,
                                    String observaciones,
                                    RestriccionesFumigacion restricciones);

    /** Sin tocar las restricciones guardadas */
    default Fumigacion actualizarFumigacion(Long id, LocalDate fecha, String producto, Long agriculturalInputId,
                                            Double dosis, String unidadMedida, Double areaAplicada,
                                            String observaciones) {
        return actualizarFumigacion(id, fecha, producto, agriculturalInputId, dosis, unidadMedida,
                areaAplicada, observaciones, null);
    }

    void eliminarFumigacion(Long id);
}