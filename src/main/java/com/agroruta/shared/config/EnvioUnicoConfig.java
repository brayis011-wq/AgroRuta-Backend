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
                .addPathPatterns("/api/workdays", "/api/sprayings", "/api/harvests", "/api/activities");
    }
}
