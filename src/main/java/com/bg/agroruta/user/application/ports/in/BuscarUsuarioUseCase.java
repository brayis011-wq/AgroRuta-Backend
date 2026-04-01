package com.bg.agroruta.user.application.ports.in;

import com.bg.agroruta.user.domain.Usuario;
import java.util.List;

public interface BuscarUsuarioUseCase {
    Usuario buscarPorId(Long id);
    Usuario buscarPorEmail(String email);
    List<Usuario> listarActivos();
}