package com.agroruta.crop.application.ports.out;

import com.agroruta.crop.domain.ActividadProgramada;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ActividadProgramadaRepository {

    ActividadProgramada guardar(ActividadProgramada p);

    Optional<ActividadProgramada> buscarPorId(Long id);

    List<ActividadProgramada> deSiembra(Long siembraId);

    void eliminar(Long id);

    /**
     * Fechas en que se hizo cada tipo de labor en la siembra: las actividades por su tipo (PODA, RIEGO…)
     * y las fumigaciones como FERTILIZACION (si el producto es un fertilizante) o FUMIGACION.
     */
    Map<String, List<LocalDate>> fechasHechas(Long siembraId);
}
