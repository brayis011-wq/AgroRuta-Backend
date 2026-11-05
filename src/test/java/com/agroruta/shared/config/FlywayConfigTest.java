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
    @DisplayName("flyway() en una base sin insumos usa baselineVersion='1' para que V2 los cargue")
    void flywayBeanShouldUseBaselineOneForNewDatabase() {
        DataSource dataSource = mock(DataSource.class);

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            FluentConfiguration fluentConfig = buildFluentConfigMock(flywayStatic);
            when(fluentConfig.load()).thenReturn(mock(Flyway.class));

            config.flyway(dataSource);

            verify(fluentConfig).baselineVersion("1");
        }
    }

    @Test
    @DisplayName("flyway() no debe tumbar la app si una migración falla")
    void flywayBeanShouldNotThrowWhenMigrateFails() {
        DataSource dataSource = mock(DataSource.class);

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            FluentConfiguration fluentConfig = buildFluentConfigMock(flywayStatic);
            Flyway flywayMock = mock(Flyway.class);
            when(fluentConfig.load()).thenReturn(flywayMock);
            when(flywayMock.migrate()).thenThrow(new org.flywaydb.core.api.FlywayException("falló"));

            assertThat(config.flyway(dataSource)).isSameAs(flywayMock);
        }
    }

    @Test
    @DisplayName("db/migration debe traer V1 con todas las tablas")
    void v1ShouldCreateAllTables() throws Exception {
        try (var in = getClass().getResourceAsStream("/db/migration/V1__esquema_inicial.sql")) {
            assertThat(in).isNotNull();
            String sql = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertThat(sql.split("CREATE TABLE IF NOT EXISTS `", -1)).hasSize(20);
            assertThat(sql).doesNotContain("DROP TABLE");
        }
    }

    @Test
    @DisplayName("lineaBase() es 2 cuando la base ya tiene insumos (producción)")
    void lineaBaseConDatos() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        java.sql.Connection c = mock(java.sql.Connection.class);
        java.sql.Statement st = mock(java.sql.Statement.class);
        java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
        when(dataSource.getConnection()).thenReturn(c);
        when(c.createStatement()).thenReturn(st);
        when(st.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(71L);

        assertThat(FlywayConfig.lineaBase(dataSource)).isEqualTo("2");
    }

    @Test
    @DisplayName("lineaBase() es 1 cuando no se puede consultar la tabla")
    void lineaBaseSinTabla() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("sin tabla"));

        assertThat(FlywayConfig.lineaBase(dataSource)).isEqualTo("1");
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