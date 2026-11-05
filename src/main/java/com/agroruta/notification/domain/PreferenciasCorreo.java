package com.agroruta.notification.domain;

/**
 * Qué correos opcionales quiere recibir un usuario. Los de seguridad (recuperar contraseña,
 * contraseña cambiada, cuenta bloqueada) llegan siempre.
 *
 * @param alertasCultivo     resumen diario: fumigaciones, labores, reentrada, carencia y clima
 * @param avisosTrabajadores solicitudes de acceso al portal de sus trabajadores
 */
public record PreferenciasCorreo(Long usuarioId, boolean alertasCultivo, boolean avisosTrabajadores) {

    /** Si el usuario nunca las cambió, recibe todo */
    public static PreferenciasCorreo porDefecto(Long usuarioId) {
        return new PreferenciasCorreo(usuarioId, true, true);
    }
}
