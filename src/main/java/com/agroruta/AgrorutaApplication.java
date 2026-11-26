package com.agroruta;

import com.agroruta.shared.util.ZonaHoraria;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgrorutaApplication {

    public static void main(String[] args) {
        ZonaHoraria.fijarPorDefecto();
        SpringApplication.run(AgrorutaApplication.class, args);
    }
}
