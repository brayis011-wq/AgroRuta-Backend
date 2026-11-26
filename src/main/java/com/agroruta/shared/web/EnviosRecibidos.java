package com.agroruta.shared.web;

/** Claves de los envíos que ya se recibieron, para no guardar dos veces el mismo registro. */
public interface EnviosRecibidos {

    /** Anota la clave. Devuelve false si esa persona ya la había mandado. */
    boolean anotar(Long usuarioId, String clave);

    /** El registro no se guardó (error o regla de negocio): la clave queda libre para reintentar. */
    void olvidar(Long usuarioId, String clave);
}
