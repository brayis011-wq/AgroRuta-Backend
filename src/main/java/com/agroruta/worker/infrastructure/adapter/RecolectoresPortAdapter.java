package com.agroruta.worker.infrastructure.adapter;

import com.agroruta.crop.application.ports.out.RecolectoresPort;
import com.agroruta.crop.domain.Recolector;
import com.agroruta.crop.domain.RecolectorCosecha;
import com.agroruta.worker.application.ports.in.JornalUseCase;
import com.agroruta.worker.application.ports.in.JornalUseCase.Recoleccion;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.Jornal;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Los recolectores de una cosecha son jornales del equipo: este adaptador los crea, corrige y quita. */
@Component
public class RecolectoresPortAdapter implements RecolectoresPort {

    private final JornalUseCase jornales;

    public RecolectoresPortAdapter(JornalUseCase jornales) {
        this.jornales = jornales;
    }

    @Override
    public void guardar(Long cosechaId, Long siembraId, LocalDate fecha, List<Recolector> recolectores) {
        jornales.guardarRecolectores(cosechaId, siembraId, fecha, recolectores.stream()
                .map(r -> new Recoleccion(r.trabajadorId(), r.porKilo(), r.kilos(), r.valorKilo(), r.valorDia()))
                .toList());
    }

    @Override
    public Map<Long, List<RecolectorCosecha>> deCosechas(Collection<Long> cosechaIds) {
        if (cosechaIds == null || cosechaIds.isEmpty()) return Map.of();
        List<Jornal> lista = jornales.jornalesDeCosechas(cosechaIds);
        Map<Long, EstadoNomina> estados = jornales.estadosDeNomina(lista.stream().map(Jornal::getId).toList());
        Map<Long, List<RecolectorCosecha>> resultado = new LinkedHashMap<>();
        lista.stream()
                .sorted(Comparator.comparing((Jornal j) -> j.getTrabajador().getNombreCompleto(), String.CASE_INSENSITIVE_ORDER))
                .forEach(j -> {
                    EstadoNomina estado = estados.get(j.getId());
                    boolean editable = !j.isLiquidado() && (estado == null || estado == EstadoNomina.PENDIENTE || estado == EstadoNomina.ANULADA);
                    resultado.computeIfAbsent(j.getCosechaId(), k -> new java.util.ArrayList<>()).add(new RecolectorCosecha(
                            j.getId(), j.getTrabajador().getId(), j.getTrabajador().getNombreCompleto(),
                            j.getKilos() != null ? "KILO" : "DIA", j.getKilos() != null ? j.getKilos() : j.getKilosRecogidos(),
                            j.getValorKilo(), j.getValorJornal(),
                            j.isLiquidado() && estado == null ? "PAGADA" : estado == null ? null : estado.name(), editable));
                });
        return resultado;
    }

    @Override
    public void quitar(Long cosechaId) {
        jornales.quitarRecolectores(cosechaId);
    }
}
