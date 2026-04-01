package com.bg.agroruta.user.application.ports.in;

import com.bg.agroruta.user.domain.Usuario;

public interface RegistrarUsuarioUseCase {
    Usuario registrar(Usuario usuario);
}