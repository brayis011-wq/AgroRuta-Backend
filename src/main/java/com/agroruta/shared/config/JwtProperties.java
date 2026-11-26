package com.agroruta.shared.config;

import io.jsonwebtoken.io.Decoders;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Secreto y duración del JWT. Se validan al arrancar: sin {@code JWT_SECRET} o con uno corto, el despliegue
 * falla en Render en vez de fallar en el primer inicio de sesión.
 *
 * @param secret     clave en Base64 de al menos 256 bits (lo que pide HS256)
 * @param expiration milisegundos que dura el token de acceso
 */
@Validated
@ConfigurationProperties("jwt")
public record JwtProperties(@NotBlank String secret, @Positive long expiration) {

    static final int BYTES_MINIMOS = 32;

    public JwtProperties {
        if (secret != null && !secret.isBlank() && Decoders.BASE64.decode(secret).length < BYTES_MINIMOS) {
            throw new IllegalArgumentException("jwt.secret debe tener al menos 256 bits en Base64");
        }
    }
}
