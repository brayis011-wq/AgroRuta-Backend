package com.agroruta.admin.infrastructure.web.dto;

import com.agroruta.admin.domain.EstadisticasSistema;
import com.agroruta.admin.domain.ResumenSistema;

import java.math.BigDecimal;

public record ResumenAdminResponse(
        long usuariosActivos,
        long usuariosInactivos,
        long administradores,
        long agricultores,
        long trabajadoresUsuarios,
        long fincas,
        double hectareas,
        long lotes,
        long siembrasActivas,
        double cosechaKg,
        long trabajadoresActivos,
        long nominasPorPagar,
        BigDecimal totalPagado
) {
    public static ResumenAdminResponse from(ResumenSistema r) {
        EstadisticasSistema e = r.estadisticas();
        return new ResumenAdminResponse(
                r.usuariosActivos(), r.usuariosInactivos(), r.administradores(),
                r.agricultores(), r.trabajadoresUsuarios(),
                e.fincas(), e.hectareas(), e.lotes(), e.siembrasActivas(), e.cosechaKg(),
                e.trabajadoresActivos(), e.nominasPorPagar(), e.totalPagado());
    }
}
