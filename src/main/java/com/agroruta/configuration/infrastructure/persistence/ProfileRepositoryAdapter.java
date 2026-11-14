package com.agroruta.configuration.infrastructure.persistence;

import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.configuration.domain.Perfil;
import com.agroruta.configuration.application.ports.out.ProfileRepository;
import com.agroruta.user.infrastructure.persistence.JpaUsuarioRepository;
import com.agroruta.user.infrastructure.persistence.UsuarioEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProfileRepositoryAdapter implements ProfileRepository {

    private final JpaUsuarioRepository jpaUsuarioRepository;
    private final JpaFotoPerfilRepository jpaFotoPerfilRepository;

    public ProfileRepositoryAdapter(JpaUsuarioRepository jpaUsuarioRepository,
                                    JpaFotoPerfilRepository jpaFotoPerfilRepository) {
        this.jpaUsuarioRepository = jpaUsuarioRepository;
        this.jpaFotoPerfilRepository = jpaFotoPerfilRepository;
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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        entity.setNombre(perfil.getNombre());
        entity.setTelefono(perfil.getTelefono());
        UsuarioEntity guardado = jpaUsuarioRepository.save(entity);
        guardarFoto(guardado.getId(), perfil.getFotoPerfil());

        return toDomain(guardado, perfil.getFotoPerfil());
    }

    @Override
    public void cambiarPassword(Long id, String nuevaPasswordEncriptada) {
        UsuarioEntity entity = jpaUsuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        entity.setPassword(nuevaPasswordEncriptada);
        jpaUsuarioRepository.save(entity);
    }

    @Override
    public String obtenerPasswordHash(Long id) {
        return jpaUsuarioRepository.findById(id)
                .map(UsuarioEntity::getPassword)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    /** La foto vive en su propia tabla: sin foto (null o vacía) no queda fila */
    private void guardarFoto(Long usuarioId, String foto) {
        if (foto == null || foto.isBlank()) {
            if (jpaFotoPerfilRepository.existsById(usuarioId)) jpaFotoPerfilRepository.deleteById(usuarioId);
            return;
        }
        jpaFotoPerfilRepository.save(new FotoPerfilEntity(usuarioId, foto));
    }

    private Perfil toDomain(UsuarioEntity entity) {
        String foto = jpaFotoPerfilRepository.findById(entity.getId())
                .map(FotoPerfilEntity::getFoto)
                .orElse(null);
        return toDomain(entity, foto);
    }

    private Perfil toDomain(UsuarioEntity entity, String foto) {
        return new Perfil(
                entity.getId(),
                entity.getNombre(),
                entity.getEmail(),
                entity.getTelefono(),
                foto,
                entity.getRol()
        );
    }
}
