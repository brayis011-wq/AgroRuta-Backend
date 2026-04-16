package com.agroruta.user.application.ports.in;

import com.agroruta.user.domain.Usuario;
import java.util.List;

public interface BuscarUsuarioUseCase {
    Usuario buscarPorId(Long id);
    Usuario buscarPorEmail(String email);
    List<Usuario> listarActivos();
}