package com.bg.agroruta.user.application;

import com.bg.agroruta.shared.exception.BusinessException;
import com.bg.agroruta.shared.exception.ResourceNotFoundException;
import com.bg.agroruta.user.application.ports.in.BuscarUsuarioUseCase;
import com.bg.agroruta.user.application.ports.in.GestionarUsuarioUseCase;
import com.bg.agroruta.user.application.ports.in.RegistrarUsuarioUseCase;
import com.bg.agroruta.user.domain.Usuario;
import com.bg.agroruta.user.domain.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements RegistrarUsuarioUseCase, BuscarUsuarioUseCase, GestionarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;

    public UserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Usuario registrar(Usuario usuario) {
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new BusinessException("El correo electrónico ya está registrado en el sistema.");
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el correo: " + email));
    }

    @Override
    public List<Usuario> listarActivos() {
        return usuarioRepository.findAllActivos();
    }

    @Override
    public void desactivarUsuario(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.desactivar();
        usuarioRepository.save(usuario);
    }

    @Override
    public void activarUsuario(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.activar();
        usuarioRepository.save(usuario);
    }
}