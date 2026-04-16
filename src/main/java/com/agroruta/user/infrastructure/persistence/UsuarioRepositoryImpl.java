package com.agroruta.user.infrastructure.persistence;

import com.agroruta.user.domain.Usuario;
import com.agroruta.user.domain.UsuarioRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// ¡ESTA ANOTACIÓN ES LA CLAVE!
// Le dice a Spring "Aquí está la implementación del repositorio que el UserService estaba pidiendo".
@Repository
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final JpaUsuarioRepository jpaRepository;

    public UsuarioRepositoryImpl(JpaUsuarioRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Usuario save(Usuario usuario) {
        // 1. Convertimos el objeto de Dominio a objeto de Base de Datos (Entidad)
        UsuarioEntity entity = toEntity(usuario);
        // 2. Guardamos en la base de datos
        UsuarioEntity savedEntity = jpaRepository.save(entity);
        // 3. Devolvemos el objeto convertido de vuelta a Dominio
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public List<Usuario> findAllActivos() {
        return jpaRepository.findByActivoTrue().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // --- MÉTODOS AUXILIARES PARA MAPEAR (Convertir) ---
    // Pasan los datos de la clase del Dominio a la clase de la Base de Datos y viceversa

    private UsuarioEntity toEntity(Usuario domain) {
        return new UsuarioEntity(
                domain.getId(),
                domain.getNombre(),
                domain.getEmail(),
                domain.getPassword(),
                domain.getRol(),
                domain.isActivo(),
                domain.getFechaCreacion()
        );
    }

    private Usuario toDomain(UsuarioEntity entity) {
        Usuario usuario = new Usuario(
                entity.getId(),
                entity.getNombre(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getRol()
        );
        // Seteamos los campos adicionales
        if (!entity.isActivo()) {
            usuario.desactivar();
        }
        usuario.setFechaCreacion(entity.getFechaCreacion());
        return usuario;
    }
}