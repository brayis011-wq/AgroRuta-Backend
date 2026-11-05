package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.EtapaSiembra;
import com.agroruta.crop.domain.Siembra;
import java.time.LocalDate;
import java.util.List;

public interface SiembraUseCase {
    Siembra registrarSiembra(LocalDate fechaSiembra, Integer cantidadPlantas,
                             Long variedadId, Long loteId);
    Siembra buscarSiembraPorId(Long id);
    /** Siembra en curso del lote */
    Siembra buscarSiembraPorLote(Long loteId);
    /** Todas las temporadas del lote, la más reciente primero */
    List<Siembra> listarHistorialPorLote(Long loteId);
    Siembra actualizarSiembra(Long id, LocalDate fechaSiembra, Integer cantidadPlantas, Long variedadId);
    /** fecha null = hoy */
    Siembra avanzarEtapa(Long siembraId, LocalDate fecha);
    Siembra retrocederEtapa(Long siembraId);
    /** Cierra la temporada; el lote queda disponible o en descanso */
    Siembra finalizarSiembra(Long siembraId, LocalDate fecha, boolean dejarLoteEnDescanso);
    List<EtapaSiembra> listarEtapas(Long siembraId);
    List<Siembra> listarSiembrasPorEstado(String estado);
    void eliminarSiembra(Long id);
}
