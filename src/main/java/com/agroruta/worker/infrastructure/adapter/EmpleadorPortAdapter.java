package com.agroruta.worker.infrastructure.adapter;

import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.user.domain.Usuario;
import com.agroruta.worker.application.ports.out.EmpleadorPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmpleadorPortAdapter implements EmpleadorPort {

    private final UsuarioRepository usuarioRepository;

    public EmpleadorPortAdapter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Optional<String> nombre(Long agricultorId) {
        return agricultorId == null ? Optional.empty()
                : usuarioRepository.findById(agricultorId).map(Usuario::getNombre);
    }
}
