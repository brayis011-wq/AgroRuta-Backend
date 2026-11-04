package com.agroruta.admin.infrastructure.persistence;

import com.agroruta.admin.application.ports.out.AdminEstadisticasPort;
import com.agroruta.admin.domain.ConteoAgricultor;
import com.agroruta.admin.domain.EstadisticasSistema;
import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.EstadoTrabajador;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

@Component
@Transactional(readOnly = true)
public class AdminEstadisticasAdapter implements AdminEstadisticasPort {

    private final AdminEstadisticasJpaRepository repository;

    public AdminEstadisticasAdapter(AdminEstadisticasJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public EstadisticasSistema estadisticasSistema() {
        return new EstadisticasSistema(
                repository.contarFincas(),
                aDouble(repository.sumarHectareas()),
                repository.contarLotes(),
                repository.contarSiembrasActivas(EstadoCultivo.FINALIZADO),
                aDouble(repository.sumarCosechaKg()),
                repository.contarTrabajadores(EstadoTrabajador.ACTIVO),
                repository.contarNominas(List.of(EstadoNomina.PENDIENTE, EstadoNomina.APROBADA)),
                aBigDecimal(repository.sumarPagos())
        );
    }

    @Override
    public Map<Long, ConteoAgricultor> conteosPorAgricultor() {
        Map<Long, ConteoAgricultor> conteos = new HashMap<>();

        for (Object[] fila : repository.fincasPorAgricultor()) {
            actualizar(conteos, fila[0], c -> new ConteoAgricultor(
                    aLong(fila[1]), aDouble(fila[2]), c.trabajadoresActivos(), c.nominas(), c.totalPagado()));
        }
        for (Object[] fila : repository.trabajadoresPorAgricultor(EstadoTrabajador.ACTIVO)) {
            actualizar(conteos, fila[0], c -> new ConteoAgricultor(
                    c.fincas(), c.hectareas(), aLong(fila[1]), c.nominas(), c.totalPagado()));
        }
        for (Object[] fila : repository.nominasPorAgricultor()) {
            actualizar(conteos, fila[0], c -> new ConteoAgricultor(
                    c.fincas(), c.hectareas(), c.trabajadoresActivos(), aLong(fila[1]), c.totalPagado()));
        }
        for (Object[] fila : repository.pagosPorAgricultor()) {
            actualizar(conteos, fila[0], c -> new ConteoAgricultor(
                    c.fincas(), c.hectareas(), c.trabajadoresActivos(), c.nominas(), aBigDecimal(fila[1])));
        }
        return conteos;
    }

    private static void actualizar(Map<Long, ConteoAgricultor> conteos, Object id,
                                   UnaryOperator<ConteoAgricultor> cambio) {
        if (id == null) return; // datos viejos sin agricultor asignado
        Long agricultorId = aLong(id);
        conteos.put(agricultorId, cambio.apply(conteos.getOrDefault(agricultorId, ConteoAgricultor.VACIO)));
    }

    static long aLong(Object valor) {
        return valor instanceof Number n ? n.longValue() : 0L;
    }

    static double aDouble(Object valor) {
        return valor instanceof Number n ? n.doubleValue() : 0.0;
    }

    static BigDecimal aBigDecimal(Object valor) {
        if (valor instanceof BigDecimal b) return b;
        if (valor instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
