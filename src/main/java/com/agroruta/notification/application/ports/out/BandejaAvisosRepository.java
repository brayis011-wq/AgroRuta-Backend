package com.agroruta.notification.application.ports.out;

import com.agroruta.notification.domain.AvisoBandeja;
import java.time.Instant;
import java.util.List;

public interface BandejaAvisosRepository {

    /** Guarda el aviso; si ya había uno con la misma etiqueta para esa persona, lo reemplaza y queda sin leer. */
    void guardar(AvisoBandeja.Nuevo aviso, Instant ahora);

    /** Los más recientes primero */
    List<AvisoBandeja> recientes(Long usuarioId, int limite);

    int noLeidos(Long usuarioId);

    /** Solo si el aviso es de esa persona. Devuelve si lo encontró. */
    boolean marcarLeido(Long usuarioId, Long avisoId, Instant ahora);

    int marcarTodosLeidos(Long usuarioId, Instant ahora);
}
