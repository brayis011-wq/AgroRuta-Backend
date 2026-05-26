package com.agroruta.shared.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FlywayConfig")
class FlywayConfigTest {

    private final FlywayConfig config = new FlywayConfig();

    @Test
    @DisplayName("flyway() debe retornar la instancia creada por Flyway.configure()")
    void flywayBeanShouldReturnConfiguredFlyway() {
        DataSource dataSource = mock(DataSource.class);

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            FluentConfiguration fluentConfig = buildFluentConfigMock(flywayStatic);
            Flyway flywayMock = mock(Flyway.class);
            when(fluentConfig.load()).thenReturn(flywayMock);

            Flyway result = config.flyway(dataSource);

            assertThat(result).isSameAs(flywayMock);
        }
    }

    @Test
    @DisplayName("flyway() debe invocar migrate() una vez tras configurar")
    void flywayBeanShouldCallMigrate() {
        DataSource dataSource = mock(DataSource.class);

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            FluentConfiguration fluentConfig = buildFluentConfigMock(flywayStatic);
            Flyway flywayMock = mock(Flyway.class);
            when(fluentConfig.load()).thenReturn(flywayMock);

            config.flyway(dataSource);

            verify(flywayMock).migrate();
        }
    }

    @Test
    @DisplayName("flyway() debe configurar el dataSource recibido")
    void flywayBeanShouldUseProvidedDataSource() {
        DataSource dataSource = mock(DataSource.class);

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            FluentConfiguration fluentConfig = buildFluentConfigMock(flywayStatic);
            when(fluentConfig.load()).thenReturn(mock(Flyway.class));

            config.flyway(dataSource);

            verify(fluentConfig).dataSource(dataSource);
        }
    }

    @Test
    @DisplayName("flyway() debe apuntar a classpath:db/migration")
    void flywayBeanShouldUseMigrationLocation() {
        DataSource dataSource = mock(DataSource.class);

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            FluentConfiguration fluentConfig = buildFluentConfigMock(flywayStatic);
            when(fluentConfig.load()).thenReturn(mock(Flyway.class));

            config.flyway(dataSource);

            verify(fluentConfig).locations("classpath:db/migration");
        }
    }

    @Test
    @DisplayName("flyway() debe habilitar baselineOnMigrate=true")
    void flywayBeanShouldEnableBaselineOnMigrate() {
        DataSource dataSource = mock(DataSource.class);

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            FluentConfiguration fluentConfig = buildFluentConfigMock(flywayStatic);
            when(fluentConfig.load()).thenReturn(mock(Flyway.class));

            config.flyway(dataSource);

            verify(fluentConfig).baselineOnMigrate(true);
        }
    }

    @Test
    @DisplayName("flyway() debe establecer baselineVersion='1'")
    void flywayBeanShouldSetBaselineVersionToOne() {
        DataSource dataSource = mock(DataSource.class);

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            FluentConfiguration fluentConfig = buildFluentConfigMock(flywayStatic);
            when(fluentConfig.load()).thenReturn(mock(Flyway.class));

            config.flyway(dataSource);

            verify(fluentConfig).baselineVersion("1");
        }
    }

    private FluentConfiguration buildFluentConfigMock(MockedStatic<Flyway> flywayStatic) {
        FluentConfiguration fluentConfig = mock(FluentConfiguration.class);

        flywayStatic.when(Flyway::configure).thenReturn(fluentConfig);
        when(fluentConfig.dataSource(any(DataSource.class))).thenReturn(fluentConfig);
        when(fluentConfig.locations(anyString())).thenReturn(fluentConfig);
        when(fluentConfig.baselineOnMigrate(anyBoolean())).thenReturn(fluentConfig);
        when(fluentConfig.baselineVersion(anyString())).thenReturn(fluentConfig);

        return fluentConfig;
    }
}