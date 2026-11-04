package com.agroruta.admin.infrastructure.web.dto;

import com.agroruta.admin.domain.AgricultorDetalle;
import com.agroruta.crop.domain.Finca;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Trabajador;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AgricultorDetalleResponse(
        AgricultorAdminResponse agricultor,
        List<FincaItem> fincas,
        List<TrabajadorItem> trabajadores,
        List<NominaItem> nominas
) {
    public record FincaItem(Long id, String nombre, String ubicacion, Double hectareas) {
        static FincaItem from(Finca f) {
            return new FincaItem(f.getId(), f.getNombre(), f.getUbicacion(), f.getHectareas());
        }
    }

    public record TrabajadorItem(Long id, String nombreCompleto, String cedula, String cargo, String estado) {
        static TrabajadorItem from(Trabajador t) {
            return new TrabajadorItem(
                    t.getId(), t.getNombreCompleto(), t.getCedula(),
                    t.getCargo() != null ? t.getCargo().getNombre() : null,
                    t.getEstado() != null ? t.getEstado().name() : null);
        }
    }

    public record NominaItem(Long id, String trabajador, LocalDate periodoInicio, LocalDate periodoFin,
                             int totalJornales, BigDecimal valorTotal, String estado) {
        static NominaItem from(Nomina n) {
            return new NominaItem(
                    n.getId(),
                    n.getTrabajador() != null ? n.getTrabajador().getNombreCompleto() : null,
                    n.getPeriodoInicio(), n.getPeriodoFin(), n.getTotalJornales(), n.getValorTotal(),
                    n.getEstado() != null ? n.getEstado().name() : null);
        }
    }

    public static AgricultorDetalleResponse from(AgricultorDetalle d) {
        return new AgricultorDetalleResponse(
                AgricultorAdminResponse.from(d.resumen()),
                d.fincas().stream().map(FincaItem::from).toList(),
                d.trabajadores().stream().map(TrabajadorItem::from).toList(),
                d.nominas().stream().map(NominaItem::from).toList());
    }
}
