package com.agroruta.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Zona horaria de la base de datos")
class ZonaBaseDatosConfigTest {

    @Test
    void sinZonaEnLaUrlSeGuardaEnUtcComoSiempre() {
        try (HikariDataSource ds = new HikariDataSource()) {
            ds.setJdbcUrl("jdbc:mysql://gateway01.tidbcloud.com:4000/agroruta?sslMode=VERIFY_IDENTITY");

            ZonaBaseDatosConfig.aplicar(ds);

            assertThat(ds.getDataSourceProperties()).containsEntry("connectionTimeZone", "UTC");
        }
    }

    @Test
    void siLaUrlYaDiceLaZonaSeRespeta() {
        try (HikariDataSource ds = new HikariDataSource()) {
            ds.setJdbcUrl("jdbc:mysql://db:3306/agroruta_db?useSSL=false&serverTimezone=America/Bogota");

            ZonaBaseDatosConfig.aplicar(ds);

            assertThat(ds.getDataSourceProperties()).doesNotContainKey("connectionTimeZone");
        }
        assertThat(ZonaBaseDatosConfig.traeZona("jdbc:mysql://x/y?connectionTimeZone=UTC"))
                .isTrue();
        assertThat(ZonaBaseDatosConfig.traeZona(null)).isFalse();
    }
}
