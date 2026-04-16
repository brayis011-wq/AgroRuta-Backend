package com.agroruta.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// Extendemos de JpaRepository pasándole nuestra Entidad y el tipo de dato del ID (Long)
public interface JpaUsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    // Spring Data JPA arma la consulta SQL automáticamente solo con leer el nombre del método
    Optional<UsuarioEntity> findByEmail(String email);

    // Busca todos los usuarios donde la columna 'activo' sea true
    List<UsuarioEntity> findByActivoTrue();
}