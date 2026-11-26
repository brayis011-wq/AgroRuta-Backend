package com.agroruta.crop.application.ports.out;

import com.agroruta.crop.domain.Recolector;
import com.agroruta.crop.domain.RecolectorCosecha;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Lo que la cosecha le pide a Trabajadores: crear, corregir y quitar los jornales de quienes la recogieron.
 * Los jornales nuevos entran a la nómina pendiente de su semana, como los de la planilla.
 */
public interface RecolectoresPort {

    /**
     * Deja los recolectores de la cosecha como vienen en la lista. Los que ya están en una nómina aprobada
     * o pagada no se pueden cambiar ni quitar; los demás se vuelven a crear con los datos nuevos.
     */
    void guardar(Long cosechaId, Long siembraId, LocalDate fecha, List<Recolector> recolectores);

    /** Los recolectores de cada cosecha (las que no tienen no aparecen en el mapa) */
    Map<Long, List<RecolectorCosecha>> deCosechas(Collection<Long> cosechaIds);

    /** Al borrar la cosecha: borra los jornales que se pueden borrar y desliga los que ya están en una nómina */
    void quitar(Long cosechaId);
}
