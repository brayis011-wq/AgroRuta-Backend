package com.agroruta.user.application.ports.in;

/**
 * Lo que llega para iniciar sesión.
 *
 * @param codigo              código de la app de autenticación o de respaldo (solo con verificación en dos pasos)
 * @param recordarDispositivo con el código: no volver a pedirlo en este dispositivo (null = sí)
 */
public record DatosLogin(String email, String password, String codigo, Boolean recordarDispositivo) {

    public DatosLogin(String email, String password) {
        this(email, password, null, null);
    }
}
