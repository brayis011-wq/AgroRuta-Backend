package com.agroruta.crop.application.ports.out;

import com.agroruta.crop.domain.PresupuestoSiembra;

import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository {

    Optional<PresupuestoSiembra> deSiembra(Long siembraId);

    /** Guarda el presupuesto y vuelve a empezar los avisos */
    void guardar(PresupuestoSiembra p);

    void borrar(Long siembraId);

    void marcarAvisado(Long siembraId, int nivel);

    /** Una siembra activa con presupuesto, con su finca y lote para el aviso */
    record ConLugar(PresupuestoSiembra presupuesto, String finca, String lote, String cultivo) {}

    /** Presupuestos de las siembras activas del agricultor */
    List<ConLugar> activosDe(Long agricultorId);
}
