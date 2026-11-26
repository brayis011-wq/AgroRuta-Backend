package com.agroruta.shared.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Sesión larga del navegador (refresh token en cookie).
 *
 * @param duracionDias días que dura la sesión sin volver a pedir la contraseña
 * @param cookieSegura cookies solo por HTTPS; en local por HTTP va en false
 */
@Validated
@ConfigurationProperties("app.sesion")
public record SesionProperties(@DefaultValue("30") @Positive long duracionDias,
                               @DefaultValue("true") boolean cookieSegura) {

    public Duration duracion() {
        return Duration.ofDays(duracionDias);
    }
}
