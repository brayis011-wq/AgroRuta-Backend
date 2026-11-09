package com.agroruta.configuration.infrastructure.web;

import com.agroruta.shared.security.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.Map;
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
@RequestMapping("/api/configuration/profile")
public class ProfileController {

    private final GetProfileUseCase     obtenerPerfilUseCase;
    private final UpdateProfileUseCase  actualizarPerfilUseCase;
    private final ChangePasswordUseCase cambiarPasswordUseCase;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public ProfileController(GetProfileUseCase obtenerPerfilUseCase,
                             UpdateProfileUseCase actualizarPerfilUseCase,
                             ChangePasswordUseCase cambiarPasswordUseCase,
                             JwtService jwtService,
                             UserDetailsService userDetailsService) {
        this.obtenerPerfilUseCase    = obtenerPerfilUseCase;
        this.actualizarPerfilUseCase = actualizarPerfilUseCase;
        this.cambiarPasswordUseCase  = cambiarPasswordUseCase;
        this.jwtService              = jwtService;
        this.userDetailsService      = userDetailsService;
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

    /**
     * Cambia la contraseña. Las sesiones anteriores quedan cerradas (ver JwtService),
     * así que se devuelve un token nuevo para que quien la cambió siga conectado.
     */
    @PatchMapping("/password")
    public ResponseEntity<Map<String, String>> cambiarPassword(
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

        UserDetails actualizado = userDetailsService.loadUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("token", jwtService.generateToken(actualizado)));
    }
}