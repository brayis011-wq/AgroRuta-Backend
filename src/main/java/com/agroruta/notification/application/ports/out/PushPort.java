package com.agroruta.notification.application.ports.out;

/** Entrega un aviso ya cifrado al servicio de avisos del navegador (Google, Mozilla, Apple, Microsoft). */
public interface PushPort {

    enum Resultado {
        ENTREGADO,
        /** El navegador ya no existe o quitó el permiso: la suscripción se borra */
        CADUCADA,
        FALLO
    }

    Resultado enviar(String endpoint, String autorizacion, byte[] cuerpo);
}
