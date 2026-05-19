package com.agroruta.configuration.domain;

import java.util.Optional;

public interface ProfileRepository {
    Optional<Perfil> buscarPorEmail(String email);
    Optional<Perfil> buscarPorId(Long id);
    Perfil guardar(Perfil perfil);
    void cambiarPassword(Long id, String nuevaPasswordEncriptada);
    String obtenerPasswordHash(Long id);
}