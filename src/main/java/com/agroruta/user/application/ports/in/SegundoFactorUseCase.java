package com.agroruta.user.application.ports.in;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Segundo factor de autenticación (opcional por cuenta): además de la contraseña, un código de
 * 6 dígitos de una app de autenticación.
 */
public interface SegundoFactorUseCase {

    record Estado(boolean activo, int codigosRespaldo) {}

    /** @param qr imagen del QR lista para un &lt;img&gt; (data URI) */
    record Preparacion(String secreto, String uri, String qr) {}

    boolean activo(Long usuarioId);

    Estado estado(Long usuarioId);

    /** Empieza la configuración: crea el secreto y devuelve el QR. Pide la contraseña actual. */
    Preparacion preparar(Long usuarioId, String password);

    /** Termina la configuración con un código de la app. Devuelve los códigos de respaldo (se ven una sola vez). */
    List<String> activar(Long usuarioId, String codigo);

    /** Lo quita. Pide la contraseña y un código (de la app o de respaldo). */
    void desactivar(Long usuarioId, String password, String codigo);

    /**
     * ¿El código sirve para entrar? Acepta el de la app o uno de respaldo (que queda gastado).
     * Si la cuenta no tiene segundo factor, responde true.
     */
    boolean verificar(Long usuarioId, String codigo);

    /** Cuánto tiempo deja de pedirse el código en un dispositivo después de escribirlo bien */
    Duration RECORDAR_DISPOSITIVO = Duration.ofDays(30);

    /**
     * Marca firmada para que este dispositivo no vuelva a pedir el código durante
     * {@link #RECORDAR_DISPOSITIVO}. Vacío si la cuenta no tiene segundo factor activo.
     * Deja de valer sola si se cambia la contraseña o se vuelve a configurar el segundo factor.
     */
    Optional<String> recordarDispositivo(Long usuarioId);

    /** ¿La marca es de esta cuenta, no venció y sigue valiendo? */
    boolean dispositivoRecordado(Long usuarioId, String marca);
}
