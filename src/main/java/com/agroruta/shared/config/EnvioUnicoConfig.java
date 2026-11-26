package com.agroruta.shared.config;

import com.agroruta.shared.web.EnvioUnicoInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Los registros que la app puede guardar sin conexión y reenviar después. */
@Configuration
public class EnvioUnicoConfig implements WebMvcConfigurer {

    private final EnvioUnicoInterceptor envioUnico;

    public EnvioUnicoConfig(EnvioUnicoInterceptor envioUnico) {
        this.envioUnico = envioUnico;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(envioUnico)
                // Lo que el teléfono puede guardar sin señal y reenviar después (ver offline-interceptor.ts del
                // frontend)
                .addPathPatterns(
                        "/api/workdays",
                        "/api/workdays/batch",
                        "/api/sprayings",
                        "/api/harvests",
                        "/api/activities",
                        "/api/sales",
                        "/api/sales/*/payments",
                        "/api/farm-expenses",
                        "/api/inventory/purchases");
    }
}
