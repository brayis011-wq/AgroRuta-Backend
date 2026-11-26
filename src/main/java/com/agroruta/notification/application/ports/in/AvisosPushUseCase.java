package com.agroruta.notification.application.ports.in;

import com.agroruta.notification.domain.AvisoPush;

/** Avisos push: notificaciones que llegan al teléfono o al navegador aunque la app esté cerrada. */
public interface AvisosPushUseCase {

    /** Clave pública del servidor que el navegador necesita para suscribirse. */
    String clavePublica();

    /** Registra (o actualiza) el navegador de esa persona. */
    void suscribir(Long usuarioId, String endpoint, String p256dh, String auth, String dispositivo);

    void desuscribir(Long usuarioId, String endpoint);

    boolean tieneSuscripcion(Long usuarioId);

    /**
     * Manda el aviso a todos los dispositivos de la persona. Nunca falla: un aviso que no sale
     * no debe afectar la operación que lo originó.
     *
     * @return a cuántos dispositivos se entregó
     */
    int enviar(Long usuarioId, AvisoPush aviso);
}
