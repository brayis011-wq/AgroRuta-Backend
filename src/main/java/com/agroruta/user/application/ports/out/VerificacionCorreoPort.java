package com.agroruta.user.application.ports.out;

import com.agroruta.user.domain.Usuario;

/** Confirmar que el correo de una cuenta es de quien se registra (lo implementa el módulo de correos). */
public interface VerificacionCorreoPort {

    /** Si está apagada (VERIFICAR_CORREO=false) las cuentas nuevas quedan confirmadas al registrarse */
    boolean activa();

    /** Manda el enlace de confirmación */
    void enviarEnlace(Usuario usuario);

    /** Usa el token del enlace y devuelve la cuenta ya confirmada. Lanza error si el enlace no sirve. */
    Usuario confirmar(String token);

    /** Vuelve a mandar el enlace si la cuenta existe y no está confirmada. No dice si existe. */
    void reenviar(String email);
}
