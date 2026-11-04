package com.agroruta.admin.domain;

import com.agroruta.crop.domain.Finca;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Trabajador;

import java.util.List;

/** Todo lo que el administrador puede ver de un agricultor. */
public record AgricultorDetalle(
        AgricultorResumen resumen,
        List<Finca> fincas,
        List<Trabajador> trabajadores,
        List<Nomina> nominas
) {}
