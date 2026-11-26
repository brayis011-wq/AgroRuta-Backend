package com.agroruta.shared.config;

import com.agroruta.shared.util.ZonaHoraria;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Un solo reloj para toda la aplicación, en la hora de Colombia. Las pruebas pasan un reloj fijo. */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.system(ZonaHoraria.COLOMBIA);
    }
}
