package com.agroruta.configuration.infrastructure.persistence;

import com.agroruta.configuration.domain.Perfil;
import com.agroruta.configuration.domain.ProfileRepository;
import com.agroruta.user.infrastructure.persistence.JpaUsuarioRepository;
import com.agroruta.user.infrastructure.persistence.UsuarioEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProfileRepositoryAdapter implements ProfileRepository {

    private final JpaUsuarioRepository jpaUsuarioRepository;

    public ProfileRepositoryAdapter(JpaUsuarioRepository jpaUsuarioRepository) {
        this.jpaUsuarioRepository = jpaUsuarioRepository;
    }

    @Override
    public Optional<Perfil> buscarPorEmail(String email) {
        return jpaUsuarioRepository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public Optional<Perfil> buscarPorId(Long id) {
        return jpaUsuarioRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Perfil guardar(Perfil perfil) {
        UsuarioEntity entity = jpaUsuarioRepository.findById(perfil.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        entity.setNombre(perfil.getNombre());
        entity.setTelefono(perfil.getTelefono());
        entity.setFotoPerfil(perfil.getFotoPerfil());

        return toDomain(jpaUsuarioRepository.save(entity));
    }

    @Override
    public void cambiarPassword(Long id, String nuevaPasswordEncriptada) {
        UsuarioEntity entity = jpaUsuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        entity.setPassword(nuevaPasswordEncriptada);
        jpaUsuarioRepository.save(entity);
    }

    @Override
    public String obtenerPasswordHash(Long id) {
        return jpaUsuarioRepository.findById(id)
                .map(UsuarioEntity::getPassword)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private Perfil toDomain(UsuarioEntity entity) {
        return new Perfil(
                entity.getId(),
                entity.getNombre(),
                entity.getEmail(),
                entity.getTelefono(),
                entity.getFotoPerfil(),
                entity.getRol()
        );
    }
}