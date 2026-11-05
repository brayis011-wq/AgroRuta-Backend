package com.agroruta.notification.domain;

/** Un correo listo para enviar: la versión HTML y la de texto plano (para clientes sin HTML). */
public record Correo(String para, String asunto, String html, String texto) {}
