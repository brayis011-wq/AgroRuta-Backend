package com.agroruta.user.domain;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {

    Usuario save(Usuario usuario);
    Optional<Usuario> findById(Long id);
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findAllActivos();

    /** Todos los usuarios, activos e inactivos, ordenados por id. */
    List<Usuario> findAll();

    /** Cuántos usuarios activos tienen el rol dado. */
    long countActivosPorRol(Rol rol);
}
