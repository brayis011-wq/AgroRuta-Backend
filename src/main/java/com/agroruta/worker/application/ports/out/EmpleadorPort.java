package com.agroruta.worker.application.ports.out;

import java.util.Optional;

/** Nombre del agricultor que emplea a un trabajador. Lo implementa un adaptador sobre el módulo de usuarios. */
public interface EmpleadorPort {
    Optional<String> nombre(Long agricultorId);
}
