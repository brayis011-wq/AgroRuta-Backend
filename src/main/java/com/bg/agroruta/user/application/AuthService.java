package com.bg.agroruta.user.application;

import com.bg.agroruta.shared.security.JwtService;
import com.bg.agroruta.user.domain.Rol;
import com.bg.agroruta.user.domain.Usuario;
import com.bg.agroruta.user.domain.UsuarioService;
import com.bg.agroruta.user.infrastructure.security.CustomUserDetails;
import com.bg.agroruta.user.infrastructure.web.dto.AuthResponse;
import com.bg.agroruta.user.infrastructure.web.dto.LoginRequest;
import com.bg.agroruta.user.infrastructure.web.dto.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioService usuarioService,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse registrar(RegisterRequest request) {
        Usuario usuario = new Usuario(
                null,
                request.getNombre(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                Rol.valueOf(request.getRol())
        );

        Usuario guardado = usuarioService.registrar(usuario);

        // Envolvemos el Usuario en CustomUserDetails para que JwtService lo entienda
        CustomUserDetails userDetails = new CustomUserDetails(guardado);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                guardado.getEmail(),
                guardado.getNombre(),
                guardado.getRol().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Usuario usuario = usuarioService.buscarPorEmail(request.getEmail());

        // Igual aquí — envolvemos en CustomUserDetails
        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol().name()
        );
    }
}