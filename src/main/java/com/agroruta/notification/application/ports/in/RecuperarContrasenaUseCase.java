package com.agroruta.notification.application.ports.in;

public interface RecuperarContrasenaUseCase {
    /** Manda el enlace si el correo es de una cuenta activa. Nunca dice si la cuenta existe. */
    void solicitar(String email);

    /** Cambia la contraseña con el token del enlace. */
    void restablecer(String token, String nuevaPassword);
}
