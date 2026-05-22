package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.Fumigacion;
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
                                   Long siembraId);

    List<Fumigacion> listarFumigacionesPorSiembra(Long siembraId);
}