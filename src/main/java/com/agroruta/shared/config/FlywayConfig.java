package com.agroruta.shared.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("local")
public class FlywayConfig {

    @Bean
    @DependsOn("entityManagerFactory") // ✅ Espera a que Hibernate cree las tablas
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1") // V1 = tablas de Hibernate, Flyway empieza en V2
                .load();

        flyway.migrate();
        return flyway;
    }
}