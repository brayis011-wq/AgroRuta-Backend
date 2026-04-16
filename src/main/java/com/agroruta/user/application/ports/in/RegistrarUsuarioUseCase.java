package com.agroruta.user.application.ports.in;

import com.agroruta.user.domain.Usuario;

public interface RegistrarUsuarioUseCase {
    Usuario registrar(Usuario usuario);
}