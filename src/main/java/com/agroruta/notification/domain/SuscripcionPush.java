package com.agroruta.notification.domain;

/** Un navegador o teléfono donde la persona activó los avisos push. */
public record SuscripcionPush(Long id, Long usuarioId, String endpoint, String p256dh, String auth, String dispositivo) {}
