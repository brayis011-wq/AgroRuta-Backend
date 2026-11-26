package com.agroruta.user.infrastructure.security;

import com.agroruta.shared.security.JwtService;
import com.agroruta.user.application.ports.out.TokenPort;
import com.agroruta.user.domain.Usuario;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenAdapter implements TokenPort {

    private final JwtService jwtService;

    public JwtTokenAdapter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public String generarToken(Usuario usuario) {
        return jwtService.generateToken(new CustomUserDetails(usuario));
    }
}
