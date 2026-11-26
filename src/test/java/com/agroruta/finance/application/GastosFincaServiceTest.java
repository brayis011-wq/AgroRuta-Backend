package com.agroruta.finance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agroruta.finance.application.ports.out.GastosFincaRepository;
import com.agroruta.finance.domain.GastoFinca;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("Gastos generales de la finca")
class GastosFincaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 25);

    private GastosFincaRepository repo;
    private GastosFincaService servicio;

    @BeforeEach
    void preparar() {
        repo = mock(GastosFincaRepository.class);
        servicio = new GastosFincaService(repo);
        when(repo.crear(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repo.actualizar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void guardaElGastoConLaDescripcionLimpiaYElValorRedondeado() {
        servicio.registrar(3L, HOY, GastoFinca.Categoria.TRANSPORTE, "  Flete del abono ", new BigDecimal("85000.456"));

        ArgumentCaptor<GastoFinca> gasto = ArgumentCaptor.forClass(GastoFinca.class);
        verify(repo).crear(gasto.capture());
        assertThat(gasto.getValue().descripcion()).isEqualTo("Flete del abono");
        assertThat(gasto.getValue().monto()).isEqualByComparingTo("85000.46");
        assertThat(gasto.getValue().categoria()).isEqualTo(GastoFinca.Categoria.TRANSPORTE);
    }

    @Test
    void sinCategoriaQuedaEnOtro() {
        GastoFinca g = servicio.registrar(3L, HOY, null, "Candado", new BigDecimal("20000"));

        assertThat(g.categoria()).isEqualTo(GastoFinca.Categoria.OTRO);
    }

    @Test
    void rechazaGastosIncompletos() {
        assertThatThrownBy(() -> servicio.registrar(null, HOY, null, "Algo", BigDecimal.TEN))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("finca");
        assertThatThrownBy(() -> servicio.registrar(3L, null, null, "Algo", BigDecimal.TEN))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("fecha");
        assertThatThrownBy(() -> servicio.registrar(3L, HOY, null, "Algo", BigDecimal.ZERO))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("mayor a 0");
        assertThatThrownBy(() -> servicio.registrar(3L, HOY, null, "   ", BigDecimal.TEN))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("en qué se gastó");
        verify(repo, never()).crear(any());
    }

    @Test
    void editarOBorrarUnGastoQueNoExisteEsNoEncontrado() {
        when(repo.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(99L, 3L, HOY, null, "Algo", BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> servicio.eliminar(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(repo, never()).eliminar(any());
    }
}
