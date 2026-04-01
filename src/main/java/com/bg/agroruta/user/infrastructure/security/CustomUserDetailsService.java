package com.bg.agroruta.user.infrastructure.security;

import com.bg.agroruta.user.domain.Usuario;
import com.bg.agroruta.user.domain.UsuarioService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// @Service es VITAL: así Spring sabe que esta es la clase que debe usar en el SecurityConfig
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioService usuarioService;

    // Inyectamos tu servicio de dominio (el que ya tiene la lógica de búsqueda)
    public CustomUserDetailsService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            // Buscamos al usuario usando la lógica de AgroRuta
            Usuario usuario = usuarioService.buscarPorEmail(email);

            // Retornamos el "Adaptador" que creamos antes (CustomUserDetails)
            // que es el que entiende Spring Security
            return new CustomUserDetails(usuario);

        } catch (RuntimeException e) {
            // Si el usuario no existe, lanzamos esta excepción específica de seguridad
            throw new UsernameNotFoundException("No se encontró el usuario con el correo: " + email);
        }
    }
}