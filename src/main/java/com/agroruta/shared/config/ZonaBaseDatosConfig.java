package com.agroruta.shared.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Zona en la que la base guarda las fechas con hora.
 *
 * <p>La JVM corre en hora de Colombia (ver {@link com.agroruta.shared.util.ZonaHoraria}), pero en producción
 * (TiDB Cloud) las horas siempre se guardaron en UTC: el DB_URL de Render no dice la zona y el servidor corría
 * en UTC. Sin esto, el driver de MySQL tomaría la zona de la JVM y las horas nuevas quedarían 5 horas
 * corridas frente a las viejas. Si el DB_URL ya trae {@code serverTimezone} o {@code connectionTimeZone}
 * (como el de Docker local, en America/Bogota), se respeta.
 */
@Configuration
public class ZonaBaseDatosConfig {

    static final String ZONA_GUARDADA = "UTC";

    private static final Logger log = LoggerFactory.getLogger(ZonaBaseDatosConfig.class);

    @Bean
    public static BeanPostProcessor zonaDeLaBaseDeDatos() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nombre) {
                if (bean instanceof HikariDataSource ds) aplicar(ds);
                return bean;
            }
        };
    }

    static void aplicar(HikariDataSource ds) {
        if (traeZona(ds.getJdbcUrl())) return;
        ds.addDataSourceProperty("connectionTimeZone", ZONA_GUARDADA);
        log.info("La URL de la base no dice su zona horaria: las fechas con hora se guardan en {}", ZONA_GUARDADA);
    }

    static boolean traeZona(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(java.util.Locale.ROOT);
        return u.contains("servertimezone=") || u.contains("connectiontimezone=");
    }
}
