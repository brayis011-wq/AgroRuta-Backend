package com.agroruta.worker.infrastructure.adapter;

import com.agroruta.crop.application.ports.out.ManoObraPort;
import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.domain.JornalRepository;
import com.agroruta.worker.domain.Trabajador;
import org.springframework.stereotype.Component;

import java.util.List;

/** Expone los jornales al módulo de cultivos (costo de mano de obra por siembra). */
@Component
public class ManoObraPortAdapter implements ManoObraPort {

    private final JornalRepository jornalRepository;

    public ManoObraPortAdapter(JornalRepository jornalRepository) {
        this.jornalRepository = jornalRepository;
    }

    @Override
    public List<JornalDeSiembra> jornalesDeSiembra(Long siembraId) {
        if (siembraId == null) return List.of();
        return jornalRepository.buscarPorCultivo(siembraId).stream()
                .map(ManoObraPortAdapter::aJornal)
                .toList();
    }

    private static JornalDeSiembra aJornal(Jornal j) {
        Trabajador t = j.getTrabajador();
        String nombre = t == null ? null
                : ((t.getNombre() == null ? "" : t.getNombre()) + " " + (t.getApellido() == null ? "" : t.getApellido())).trim();
        return new JornalDeSiembra(j.getFecha(), t == null ? null : t.getId(), nombre, j.getValorJornal());
    }
}
