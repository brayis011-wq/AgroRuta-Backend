package com.agroruta.cultivo.application.ports.in;

import com.agroruta.cultivo.domain.Fumigacion;
import java.time.LocalDate;
import java.util.List;

public interface FumigacionUseCase {
    Fumigacion registrarFumigacion(LocalDate fecha, String producto, Double dosis,
                                   String unidadMedida, Double areaAplicada,
                                   String observaciones, Long siembraId);
    List<Fumigacion> listarFumigacionesPorSiembra(Long siembraId);
}