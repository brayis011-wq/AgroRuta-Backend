package com.bg.agroruta.user.domain;

import java.util.List;

public interface UsuarioService {

    Usuario registrar(Usuario usuario);

    Usuario buscarPorId(Long id);

    Usuario buscarPorEmail(String email);

    List<Usuario> listarActivos();

    void desactivarUsuario(Long id);

    void activarUsuario(Long id);
}