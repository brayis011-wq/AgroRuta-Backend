package com.agroruta.worker.infrastructure.web;

import com.agroruta.worker.domain.PagosDelMes;
import com.agroruta.shared.util.Pagina;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.infrastructure.security.CustomUserDetails;
import com.agroruta.worker.application.ports.in.JornalUseCase;
import com.agroruta.worker.application.ports.in.NominaUseCase;
import com.agroruta.worker.application.ports.in.PagoUseCase;
import com.agroruta.worker.domain.*;
import com.agroruta.worker.infrastructure.web.dto.JornalResponse;
import com.agroruta.worker.infrastructure.web.dto.NominaResponse;
import com.agroruta.worker.infrastructure.web.dto.PagoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Listas paginadas de nóminas, pagos y jornales: dueño, filtros y límites de tamaño. */
class PaginacionControllersTest {

    private final NominaUseCase nominas = mock(NominaUseCase.class);
    private final PagoUseCase pagos = mock(PagoUseCase.class);
    private final JornalUseCase jornales = mock(JornalUseCase.class);

    private Nomina nomina;
    private Jornal jornal;

    @BeforeEach
    void setUp() {
        Cargo cargo = new Cargo(1L, "Operario", "Cargo operario", new BigDecimal("80000"), true);
        Trabajador t = new Trabajador(1L, "Juan", "Pérez", "123", "300", "Calle 1",
                LocalDate.of(2020, 1, 1), TipoContrato.JORNAL, cargo);
        jornal = new Jornal(1L, LocalDate.of(2025, 1, 15), t, 10L, "Maíz", null);
        nomina = new Nomina(1L, t, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), List.of(jornal));
    }

    private static Authentication auth(Long id, Rol rol) {
        CustomUserDetails d = new CustomUserDetails(new Usuario(id, "Nombre", "a@a.com", "pw", rol));
        return new UsernamePasswordAuthenticationToken(d, null, d.getAuthorities());
    }

    private static <T> Pagina<T> pagina(T elemento) {
        return new Pagina<>(List.of(elemento), 0, 20, 1, 1);
    }

    @Test
    void nominasDelAgricultorFiltradasPorEstadoYTrabajador() {
        when(nominas.pagina(7L, 3L, EstadoNomina.PENDIENTE, 2, 20)).thenReturn(pagina(nomina));

        Pagina<NominaResponse> r = new NominaController(nominas)
                .pagina(2, null, 3L, EstadoNomina.PENDIENTE, auth(7L, Rol.Agricultor)).getBody();

        assertThat(r.contenido()).hasSize(1);
        assertThat(r.total()).isEqualTo(1);
    }

    @Test
    void elAdministradorVeTodasLasNominas() {
        when(nominas.pagina(null, null, null, 0, 20)).thenReturn(pagina(nomina));

        new NominaController(nominas).pagina(null, null, null, null, auth(1L, Rol.Administrador));

        verify(nominas).pagina(null, null, null, 0, 20);
    }

    @Test
    void elTamanoSeLimitaYLaPaginaNoEsNegativa() {
        when(nominas.pagina(7L, null, null, 0, Pagina.TAMANO_MAXIMO)).thenReturn(pagina(nomina));

        new NominaController(nominas).pagina(-3, 5000, null, null, auth(7L, Rol.Agricultor));

        verify(nominas).pagina(7L, null, null, 0, Pagina.TAMANO_MAXIMO);
    }

    @Test
    void resumenDeNominasDelAgricultor() {
        List<ResumenNominas> resumen = List.of(new ResumenNominas(EstadoNomina.PAGADA, 2, new BigDecimal("300000")));
        when(nominas.resumenPorEstado(7L, null)).thenReturn(resumen);

        assertThat(new NominaController(nominas).resumen(null, auth(7L, Rol.Agricultor)).getBody()).isEqualTo(resumen);
    }

    @Test
    void pagosDelAgricultorFiltradosPorNomina() {
        Pago pago = new Pago(4L, nomina, LocalDate.of(2025, 2, 1), new BigDecimal("80000"), MetodoPago.EFECTIVO, null);
        when(pagos.pagina(7L, 1L, 0, 10)).thenReturn(pagina(pago));

        Pagina<PagoResponse> r = new PagoController(pagos).pagina(0, 10, 1L, auth(7L, Rol.Agricultor)).getBody();

        assertThat(r.contenido()).singleElement().satisfies(p -> assertThat(p.id).isEqualTo(4L));
    }

    @Test
    void resumenDePagosDelAgricultor() {
        ResumenPagos resumen = new ResumenPagos(3, new BigDecimal("240000"), 2);
        when(pagos.resumen(7L, null)).thenReturn(resumen);

        assertThat(new PagoController(pagos).resumen(null, auth(7L, Rol.Agricultor)).getBody()).isEqualTo(resumen);
    }

    @Test
    void pagosPorMesDelAgricultor() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        List<PagosDelMes> meses = List.of(new PagosDelMes("2026-10", new BigDecimal("90000"), 1),
                new PagosDelMes("2026-11", new BigDecimal("240000"), 3));
        when(pagos.totalesPorMes(7L, desde)).thenReturn(meses);

        assertThat(new PagoController(pagos).porMes(desde, auth(7L, Rol.Agricultor)).getBody()).isEqualTo(meses);
    }

    @Test
    void pagosPorMesParaElAdministradorSonLosDeTodos() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        when(pagos.totalesPorMes(null, desde)).thenReturn(List.of());

        assertThat(new PagoController(pagos).porMes(desde, auth(1L, Rol.Administrador)).getBody()).isEmpty();
        verify(pagos).totalesPorMes(null, desde);
    }

    @Test
    void jornalesPorTrabajadorConRangoYResumen() {
        LocalDate ini = LocalDate.of(2025, 1, 1), fin = LocalDate.of(2025, 1, 31);
        when(jornales.paginaPorTrabajador(1L, ini, fin, 1, 20)).thenReturn(pagina(jornal));
        ResumenJornales resumen = new ResumenJornales(1, new BigDecimal("80000"), 1, new BigDecimal("80000"), ini, ini);
        when(jornales.resumenPorTrabajador(1L, ini, fin)).thenReturn(resumen);

        JornalController c = new JornalController(jornales);
        Pagina<JornalResponse> r = c.paginaPorTrabajador(1L, ini, fin, 1, null).getBody();

        assertThat(r.contenido()).hasSize(1);
        assertThat(c.resumenPorTrabajador(1L, ini, fin).getBody()).isEqualTo(resumen);
    }

    @Test
    void paginaMapeaElContenidoYConservaLosTotales() {
        Pagina<Integer> p = new Pagina<>(List.of(1, 2), 3, 2, 9, 5);

        assertThat(p.map(n -> n * 10)).isEqualTo(new Pagina<>(List.of(10, 20), 3, 2, 9, 5));
        assertThat(Pagina.tamano(null)).isEqualTo(Pagina.TAMANO_POR_DEFECTO);
        assertThat(Pagina.tamano(0)).isEqualTo(Pagina.TAMANO_POR_DEFECTO);
        assertThat(Pagina.numero(null)).isZero();
    }
}
