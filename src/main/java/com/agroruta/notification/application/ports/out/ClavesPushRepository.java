package com.agroruta.notification.application.ports.out;

import java.util.Optional;

/** Dónde se guarda el par de claves con el que el servidor firma los avisos push. */
public interface ClavesPushRepository {

    record Guardadas(String publica, String privadaCifrada) {}

    Optional<Guardadas> buscar();

    void guardar(String publica, String privadaCifrada);
}
