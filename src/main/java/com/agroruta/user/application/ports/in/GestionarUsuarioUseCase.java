package com.agroruta.user.application.ports.in;

public interface GestionarUsuarioUseCase {
    void desactivarUsuario(Long id);
    void activarUsuario(Long id);
}