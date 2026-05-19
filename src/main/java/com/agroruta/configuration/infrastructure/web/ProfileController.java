package com.agroruta.configuration.infrastructure.web;

import com.agroruta.configuration.application.ports.in.ChangePasswordUseCase;
import com.agroruta.configuration.application.ports.in.GetProfileUseCase;
import com.agroruta.configuration.application.ports.in.UpdateProfileUseCase;
import com.agroruta.configuration.domain.Perfil;
import com.agroruta.configuration.infrastructure.web.dto.ChangePasswordRequest;
import com.agroruta.configuration.infrastructure.web.dto.ProfileResponse;
import com.agroruta.configuration.infrastructure.web.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuration/perfil")
public class ProfileController {

    private final GetProfileUseCase     obtenerPerfilUseCase;
    private final UpdateProfileUseCase  actualizarPerfilUseCase;
    private final ChangePasswordUseCase cambiarPasswordUseCase;

    public ProfileController(GetProfileUseCase obtenerPerfilUseCase,
                             UpdateProfileUseCase actualizarPerfilUseCase,
                             ChangePasswordUseCase cambiarPasswordUseCase) {
        this.obtenerPerfilUseCase    = obtenerPerfilUseCase;
        this.actualizarPerfilUseCase = actualizarPerfilUseCase;
        this.cambiarPasswordUseCase  = cambiarPasswordUseCase;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> obtener(
            @AuthenticationPrincipal UserDetails userDetails) {

        Perfil perfil = obtenerPerfilUseCase.obtenerPorEmail(
                userDetails.getUsername()
        );
        return ResponseEntity.ok(ProfileResponse.from(perfil));
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> actualizar(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        Perfil perfil = obtenerPerfilUseCase.obtenerPorEmail(
                userDetails.getUsername()
        );

        Perfil actualizado = actualizarPerfilUseCase.actualizar(
                perfil.getId(),
                request.getNombre(),
                request.getTelefono(),
                request.getFotoPerfil()
        );

        return ResponseEntity.ok(ProfileResponse.from(actualizado));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> cambiarPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        Perfil perfil = obtenerPerfilUseCase.obtenerPorEmail(
                userDetails.getUsername()
        );

        cambiarPasswordUseCase.cambiarPassword(
                perfil.getId(),
                request.getPasswordActual(),
                request.getNuevaPassword()
        );

        return ResponseEntity.noContent().build();
    }
}