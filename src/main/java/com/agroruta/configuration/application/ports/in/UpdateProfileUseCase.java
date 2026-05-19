package com.agroruta.configuration.application.ports.in;

import com.agroruta.configuration.domain.Perfil;

public interface UpdateProfileUseCase {
    Perfil actualizar(Long id, String nombre, String telefono, String fotoPerfil);
}