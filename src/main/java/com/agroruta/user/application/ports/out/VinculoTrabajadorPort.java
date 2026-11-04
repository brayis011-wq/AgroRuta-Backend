package com.agroruta.user.application.ports.out;

/**
 * Vincula una cuenta con rol Trabajador con el trabajador que registró un agricultor.
 * Lo implementa el módulo portal; el módulo user no conoce a los trabajadores.
 */
public interface VinculoTrabajadorPort {

    /**
     * Comprueba que la cédula exista y esté libre.
     * Lanza BusinessException con un mensaje para el usuario si no se puede usar.
     */
    void validarCedulaDisponible(String cedula);

    /** Crea la solicitud de acceso (queda pendiente hasta que el agricultor la apruebe). */
    void solicitarAcceso(Long usuarioId, String nombre, String email, String cedula);
}
