package com.agroruta.finance.infrastructure.adapter;

import com.agroruta.crop.application.ports.out.VentasSiembraPort;
import com.agroruta.finance.application.ports.out.VentasRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Expone las ventas de cada siembra al módulo de cultivos: son los ingresos de la temporada. */
@Component
public class VentasSiembraAdapter implements VentasSiembraPort {

    private final VentasRepository ventas;

    public VentasSiembraAdapter(VentasRepository ventas) {
        this.ventas = ventas;
    }

    @Override
    public List<VentaDeSiembra> ventasDeSiembra(Long siembraId) {
        if (siembraId == null) return List.of();
        return ventas.listarPorSiembra(siembraId).stream()
                .map(v -> new VentaDeSiembra(v.fecha(), v.cantidadKg(), v.total()))
                .toList();
    }
}
