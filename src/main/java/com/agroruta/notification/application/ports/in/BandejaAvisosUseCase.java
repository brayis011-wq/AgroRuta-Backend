package com.agroruta.notification.application.ports.in;

import com.agroruta.notification.domain.AvisoBandeja;
import java.util.List;

/** La bandeja de avisos de la app (la campana) de quien inició sesión. */
public interface BandejaAvisosUseCase {

    /** Guarda un aviso; nunca lanza (un aviso que no se guarda no debe romper lo que lo provocó). */
    void avisar(AvisoBandeja.Nuevo aviso);

    List<AvisoBandeja> recientes(Long usuarioId);

    int noLeidos(Long usuarioId);

    /** Lanza ResourceNotFoundException si el aviso no existe o es de otra persona */
    void marcarLeido(Long usuarioId, Long avisoId);

    void marcarTodosLeidos(Long usuarioId);
}
