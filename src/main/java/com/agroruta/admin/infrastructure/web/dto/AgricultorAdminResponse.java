package com.agroruta.admin.infrastructure.web.dto;

import com.agroruta.admin.domain.AgricultorResumen;
import com.agroruta.admin.domain.ConteoAgricultor;
import com.agroruta.user.domain.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgricultorAdminResponse(
        Long id,
        String nombre,
        String email,
        boolean activo,
        LocalDateTime fechaCreacion,
        long fincas,
        double hectareas,
        long trabajadoresActivos,
        long nominas,
        BigDecimal totalPagado
) {
    public static AgricultorAdminResponse from(AgricultorResumen a) {
        Usuario u = a.usuario();
        ConteoAgricultor c = a.conteo();
        return new AgricultorAdminResponse(
                u.getId(), u.getNombre(), u.getEmail(), u.isActivo(), u.getFechaCreacion(),
                c.fincas(), c.hectareas(), c.trabajadoresActivos(), c.nominas(), c.totalPagado());
    }
}
