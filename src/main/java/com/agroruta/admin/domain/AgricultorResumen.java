package com.agroruta.admin.domain;

import com.agroruta.user.domain.Usuario;

/** Un agricultor con sus totales. */
public record AgricultorResumen(Usuario usuario, ConteoAgricultor conteo) {}
