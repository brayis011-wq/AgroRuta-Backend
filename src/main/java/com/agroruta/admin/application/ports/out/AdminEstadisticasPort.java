package com.agroruta.admin.application.ports.out;

import com.agroruta.admin.domain.ConteoAgricultor;
import com.agroruta.admin.domain.EstadisticasSistema;

import java.util.Map;

public interface AdminEstadisticasPort {

    EstadisticasSistema estadisticasSistema();

    /** Totales por id de agricultor. Los agricultores sin datos no aparecen en el mapa. */
    Map<Long, ConteoAgricultor> conteosPorAgricultor();
}
