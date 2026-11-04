package com.agroruta.admin.infrastructure.persistence;

import com.agroruta.admin.domain.ConteoAgricultor;
import com.agroruta.admin.domain.EstadisticasSistema;
import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.worker.domain.EstadoTrabajador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminEstadisticasAdapter")
class AdminEstadisticasAdapterTest {

    @Mock private AdminEstadisticasJpaRepository repository;
    @InjectMocks private AdminEstadisticasAdapter adapter;

    @Test
    @DisplayName("estadisticasSistema convierte nulos en cero")
    void estadisticasConNulos() {
        when(repository.contarFincas()).thenReturn(0L);
        when(repository.sumarHectareas()).thenReturn(null);
        when(repository.contarLotes()).thenReturn(0L);
        when(repository.contarSiembrasActivas(EstadoCultivo.FINALIZADO)).thenReturn(0L);
        when(repository.sumarCosechaKg()).thenReturn(null);
        when(repository.contarTrabajadores(EstadoTrabajador.ACTIVO)).thenReturn(0L);
        when(repository.contarNominas(anyCollection())).thenReturn(0L);
        when(repository.sumarPagos()).thenReturn(null);

        EstadisticasSistema e = adapter.estadisticasSistema();

        assertThat(e.hectareas()).isZero();
        assertThat(e.cosechaKg()).isZero();
        assertThat(e.totalPagado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("conteosPorAgricultor combina las cuatro consultas por agricultor")
    void conteosPorAgricultor() {
        when(repository.fincasPorAgricultor()).thenReturn(List.<Object[]>of(
                new Object[]{3L, 2L, 12.5}));
        when(repository.trabajadoresPorAgricultor(EstadoTrabajador.ACTIVO)).thenReturn(List.<Object[]>of(
                new Object[]{3L, 4L}, new Object[]{8L, 1L}));
        when(repository.nominasPorAgricultor()).thenReturn(List.<Object[]>of(
                new Object[]{3L, 5L}, new Object[]{null, 9L}));
        when(repository.pagosPorAgricultor()).thenReturn(Collections.emptyList());

        Map<Long, ConteoAgricultor> conteos = adapter.conteosPorAgricultor();

        assertThat(conteos).containsOnlyKeys(3L, 8L);
        assertThat(conteos.get(3L)).isEqualTo(new ConteoAgricultor(2, 12.5, 4, 5, BigDecimal.ZERO));
        assertThat(conteos.get(8L).trabajadoresActivos()).isEqualTo(1);
        assertThat(conteos.get(8L).fincas()).isZero();
    }
}
