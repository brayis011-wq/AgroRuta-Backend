package com.agroruta.portal.application.ports.in;

import com.agroruta.portal.domain.EstadoPortal;
import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Pago;

import java.util.List;

/** Portal de solo lectura para la cuenta con rol Trabajador. */
public interface PortalTrabajadorUseCase {

    EstadoPortal estado(Long usuarioId);

    /** Pide acceso con otra cédula (por ejemplo, después de un rechazo o de un error al escribirla). */
    EstadoPortal solicitar(Long usuarioId, String cedula);

    List<Jornal> misJornales(Long usuarioId);

    List<Nomina> misNominas(Long usuarioId);

    List<Pago> misPagos(Long usuarioId);
}
