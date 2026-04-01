package com.bg.agroruta.user.infrastructure.security;

import com.bg.agroruta.user.application.ports.in.BuscarUsuarioUseCase;
import com.bg.agroruta.user.domain.Usuario;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final BuscarUsuarioUseCase buscarUsuarioUseCase;

    public CustomUserDetailsService(BuscarUsuarioUseCase buscarUsuarioUseCase) {
        this.buscarUsuarioUseCase = buscarUsuarioUseCase;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            Usuario usuario = buscarUsuarioUseCase.buscarPorEmail(email);
            return new CustomUserDetails(usuario);
        } catch (RuntimeException e) {
            throw new UsernameNotFoundException("No se encontró el usuario con el correo: " + email);
        }
    }
}