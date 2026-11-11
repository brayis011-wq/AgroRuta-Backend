package com.agroruta.notification.application.ports.out;

import com.agroruta.notification.domain.SuscripcionPush;

import java.util.List;

public interface SuscripcionPushRepository {

    /** Si ese navegador ya estaba suscrito (aunque fuera con otra cuenta) se actualiza. */
    void guardar(Long usuarioId, String endpoint, String p256dh, String auth, String dispositivo);

    /** Las más recientes primero */
    List<SuscripcionPush> deUsuario(Long usuarioId);

    boolean existe(Long usuarioId);

    void eliminar(Long id);

    void eliminarPorEndpoint(Long usuarioId, String endpoint);

    void eliminarTodas();
}
