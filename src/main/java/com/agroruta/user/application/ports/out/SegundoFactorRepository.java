package com.agroruta.user.application.ports.out;

import com.agroruta.user.domain.SegundoFactor;

import java.util.Optional;

public interface SegundoFactorRepository {

    Optional<SegundoFactor> buscar(Long usuarioId);

    SegundoFactor guardar(SegundoFactor segundoFactor);

    void borrar(Long usuarioId);
}
