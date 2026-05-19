package com.agroruta.configuration.application.service;

import com.agroruta.configuration.application.ports.in.ChangePasswordUseCase;
import com.agroruta.configuration.application.ports.in.GetProfileUseCase;
import com.agroruta.configuration.application.ports.in.UpdateProfileUseCase;
import com.agroruta.configuration.domain.Perfil;
import com.agroruta.configuration.domain.ProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService implements GetProfileUseCase,
        UpdateProfileUseCase,
        ChangePasswordUseCase {

    private final ProfileRepository perfilRepository;
    private final PasswordEncoder   passwordEncoder;

    public ProfileService(ProfileRepository perfilRepository,
                          PasswordEncoder passwordEncoder) {
        this.perfilRepository = perfilRepository;
        this.passwordEncoder  = passwordEncoder;
    }

    @Override
    public Perfil obtenerPorEmail(String email) {
        return perfilRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
    }

    @Override
    @Transactional
    public Perfil actualizar(Long id, String nombre,
                             String telefono, String fotoPerfil) {
        Perfil perfil = perfilRepository.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        perfil.actualizarDatos(nombre, telefono, fotoPerfil);
        return perfilRepository.guardar(perfil);
    }

    @Override
    @Transactional
    public void cambiarPassword(Long id, String passwordActual,
                                String nuevaPassword) {
        String hashActual = perfilRepository.obtenerPasswordHash(id);

        if (!passwordEncoder.matches(passwordActual, hashActual)) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        perfilRepository.cambiarPassword(id, passwordEncoder.encode(nuevaPassword));
    }
}