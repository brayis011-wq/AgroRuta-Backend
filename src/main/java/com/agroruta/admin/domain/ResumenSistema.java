package com.agroruta.admin.domain;

/** Lo que muestra el inicio del panel de administración. */
public record ResumenSistema(
        long usuariosActivos,
        long usuariosInactivos,
        long administradores,
        long agricultores,
        long trabajadoresUsuarios,
        EstadisticasSistema estadisticas
) {}
