package com.bg.agroruta.user.application;

import com.bg.agroruta.user.domain.Usuario;
import com.bg.agroruta.user.domain.UsuarioRepository;
import com.bg.agroruta.user.domain.UsuarioService;
import org.springframework.stereotype.Service;

import java.util.List;

// Usamos @Service para que Spring Boot sepa que debe administrar esta clase
@Service
public class UserService implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // Inyectamos el repositorio por medio del constructor (la mejor práctica en Spring)
    public UserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Usuario registrar(Usuario usuario) {
        // Regla de negocio: Validar que el correo no esté registrado previamente en AgroRuta
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado en el sistema.");
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario buscarPorId(Long id) {
        // Buscamos al usuario. Si no existe, lanzamos una excepción clara.
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el correo: " + email));
    }

    @Override
    public List<Usuario> listarActivos() {
        return usuarioRepository.findAllActivos();
    }

    @Override
    public void desactivarUsuario(Long id) {
        // 1. Buscamos al usuario
        Usuario usuario = buscarPorId(id);
        // 2. Usamos el método profesional de dominio que creamos antes
        usuario.desactivar();
        // 3. Guardamos los cambios
        usuarioRepository.save(usuario);
    }

    @Override
    public void activarUsuario(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.activar();
        usuarioRepository.save(usuario);
    }
}