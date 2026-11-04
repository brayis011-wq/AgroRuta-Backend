package com.agroruta.admin.application.ports.in;

import com.agroruta.admin.domain.AgricultorDetalle;
import com.agroruta.admin.domain.AgricultorResumen;
import com.agroruta.admin.domain.ResumenSistema;
import com.agroruta.user.domain.Usuario;

import java.util.List;

public interface AdministrarSistemaUseCase {

    ResumenSistema resumen();

    List<Usuario> listarUsuarios();

    /** Activa o desactiva una cuenta. El administrador no puede desactivarse a sí mismo. */
    Usuario cambiarEstado(Long adminId, Long usuarioId, boolean activo);

    /** Cambia el rol de una cuenta. El administrador no puede cambiar su propio rol. */
    Usuario cambiarRol(Long adminId, Long usuarioId, String rol);

    List<AgricultorResumen> listarAgricultores();

    AgricultorDetalle detalleAgricultor(Long agricultorId);
}
