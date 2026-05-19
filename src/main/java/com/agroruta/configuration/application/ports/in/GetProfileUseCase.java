package com.agroruta.configuration.application.ports.in;

import com.agroruta.configuration.domain.Perfil;

public interface GetProfileUseCase {
    Perfil obtenerPorEmail(String email);
}