package com.agroruta.shared.security;

import com.agroruta.crop.domain.CalidadCosecha;
import com.agroruta.crop.domain.Cosecha;
import com.agroruta.crop.domain.CosechaRepository;
import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.domain.FincaRepository;
import com.agroruta.crop.domain.Lote;
import com.agroruta.crop.domain.LoteRepository;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.crop.domain.SiembraRepository;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.infrastructure.security.CustomUserDetails;
import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.domain.JornalRepository;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.NominaRepository;
import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.domain.PagoRepository;
import com.agroruta.worker.domain.TipoContrato;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.domain.TrabajadorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OwnershipChecker")
class OwnershipCheckerTest {

    @Mock private FincaRepository fincaRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private SiembraRepository siembraRepository;
    @Mock private TrabajadorRepository trabajadorRepository;
    @Mock private JornalRepository jornalRepository;
    @Mock private NominaRepository nominaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private CosechaRepository cosechaRepository;
    @Mock private com.agroruta.worker.domain.LaborRepository laborRepository;

    @InjectMocks private OwnershipChecker ownership;

    private Authentication auth(Long id, Rol rol) {
        CustomUserDetails d = new CustomUserDetails(new Usuario(id, "Nombre", "a@a.com", "pw", rol));
        return new UsernamePasswordAuthenticationToken(d, null, d.getAuthorities());
    }

    private Trabajador trabajadorDe(Long agricultorId) {
        Trabajador t = new Trabajador(40L, "Juan", "Pérez", "123", "300", "Calle",
                LocalDate.of(2022, 1, 1), TipoContrato.JORNAL, null);
        t.setAgricultorId(agricultorId);
        return t;
    }

    @Test
    @DisplayName("esAgricultor: true si el id coincide, false si no")
    void esAgricultor() {
        assertThat(ownership.esAgricultor(1L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.esAgricultor(2L, auth(1L, Rol.Agricultor))).isFalse();
        assertThat(ownership.esAgricultor(null, auth(1L, Rol.Agricultor))).isFalse();
    }

    @Test
    @DisplayName("fincaPropia: dueño sí, otro usuario no")
    void fincaPropia() {
        when(fincaRepository.findById(10L))
                .thenReturn(Optional.of(new Finca(10L, "F", "U", 5.0, 1L)));

        assertThat(ownership.fincaPropia(10L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.fincaPropia(10L, auth(2L, Rol.Agricultor))).isFalse();
    }

    @Test
    @DisplayName("fincaPropia: finca inexistente devuelve false")
    void fincaInexistente() {
        when(fincaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(ownership.fincaPropia(99L, auth(1L, Rol.Agricultor))).isFalse();
    }

    @Test
    @DisplayName("lotePropio: sigue la cadena lote -> finca -> agricultor")
    void lotePropio() {
        when(loteRepository.findById(20L)).thenReturn(Optional.of(new Lote(20L, "L", 1.0, 10L)));
        when(fincaRepository.findById(10L))
                .thenReturn(Optional.of(new Finca(10L, "F", "U", 5.0, 1L)));

        assertThat(ownership.lotePropio(20L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.lotePropio(20L, auth(2L, Rol.Agricultor))).isFalse();
    }

    @Test
    @DisplayName("siembraPropia: sigue la cadena siembra -> lote -> finca -> agricultor")
    void siembraPropia() {
        Siembra siembra = new Siembra(30L, null, 100, null, 20L);
        when(siembraRepository.findById(30L)).thenReturn(Optional.of(siembra));
        when(loteRepository.findById(20L)).thenReturn(Optional.of(new Lote(20L, "L", 1.0, 10L)));
        when(fincaRepository.findById(10L))
                .thenReturn(Optional.of(new Finca(10L, "F", "U", 5.0, 1L)));

        assertThat(ownership.siembraPropia(30L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.siembraPropia(30L, auth(2L, Rol.Agricultor))).isFalse();
    }

    @Test
    @DisplayName("trabajadorPropio: dueño sí, otro usuario no, inexistente no")
    void trabajadorPropio() {
        when(trabajadorRepository.buscarPorId(40L)).thenReturn(Optional.of(trabajadorDe(1L)));
        when(trabajadorRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThat(ownership.trabajadorPropio(40L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.trabajadorPropio(40L, auth(2L, Rol.Agricultor))).isFalse();
        assertThat(ownership.trabajadorPropio(99L, auth(1L, Rol.Agricultor))).isFalse();
    }

    @Test
    @DisplayName("jornalPropio, nominaPropia y pagoPropio: dueño sí, otro no, inexistente no")
    void cadenaDeTrabajo() {
        Jornal jornal = mock(Jornal.class);
        when(jornal.getTrabajador()).thenReturn(trabajadorDe(1L));
        when(jornalRepository.buscarPorId(50L)).thenReturn(Optional.of(jornal));
        when(jornalRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        Nomina nomina = mock(Nomina.class);
        when(nomina.getTrabajador()).thenReturn(trabajadorDe(1L));
        when(nominaRepository.buscarPorId(60L)).thenReturn(Optional.of(nomina));

        Pago pago = mock(Pago.class);
        when(pago.getNomina()).thenReturn(nomina);
        when(pagoRepository.buscarPorId(70L)).thenReturn(Optional.of(pago));

        assertThat(ownership.jornalPropio(50L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.jornalPropio(50L, auth(2L, Rol.Agricultor))).isFalse();
        assertThat(ownership.jornalPropio(99L, auth(1L, Rol.Agricultor))).isFalse();

        assertThat(ownership.nominaPropia(60L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.nominaPropia(60L, auth(2L, Rol.Agricultor))).isFalse();

        assertThat(ownership.pagoPropio(70L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.pagoPropio(70L, auth(2L, Rol.Agricultor))).isFalse();
    }

    @Test
    @DisplayName("cosechaPropia: sube por siembra, lote y finca hasta el dueño")
    void cosechaPropia() {
        when(cosechaRepository.findById(80L)).thenReturn(Optional.of(
                new Cosecha(80L, LocalDate.of(2026, 5, 1), 20.0, CalidadCosecha.PRIMERA, null, 30L)));
        Siembra siembra = new Siembra();
        siembra.setId(30L);
        siembra.setLoteId(20L);
        when(siembraRepository.findById(30L)).thenReturn(Optional.of(siembra));
        when(loteRepository.findById(20L)).thenReturn(Optional.of(new Lote(20L, "L", 1.0, 10L)));
        when(fincaRepository.findById(10L)).thenReturn(Optional.of(new Finca(10L, "F", "U", 5.0, 1L)));

        assertThat(ownership.cosechaPropia(80L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.cosechaPropia(80L, auth(2L, Rol.Agricultor))).isFalse();
    }

    @Test
    @DisplayName("Administrador accede a todo sin consultar repositorios")
    void administradorAccedeATodo() {
        Authentication admin = auth(99L, Rol.Administrador);

        assertThat(ownership.esAgricultor(1L, admin)).isTrue();
        assertThat(ownership.fincaPropia(10L, admin)).isTrue();
        assertThat(ownership.lotePropio(20L, admin)).isTrue();
        assertThat(ownership.siembraPropia(30L, admin)).isTrue();
        assertThat(ownership.trabajadorPropio(40L, admin)).isTrue();
        assertThat(ownership.jornalPropio(50L, admin)).isTrue();
        assertThat(ownership.nominaPropia(60L, admin)).isTrue();
        assertThat(ownership.pagoPropio(70L, admin)).isTrue();
        verifyNoInteractions(fincaRepository, loteRepository, siembraRepository,
                trabajadorRepository, jornalRepository, nominaRepository, pagoRepository);
    }

    @Test
    @DisplayName("Sin autenticación todo es false")
    void sinAutenticacion() {
        assertThat(ownership.esAgricultor(1L, null)).isFalse();
        assertThat(ownership.fincaPropia(10L, null)).isFalse();
        assertThat(ownership.lotePropio(20L, null)).isFalse();
        assertThat(ownership.siembraPropia(30L, null)).isFalse();
        assertThat(ownership.trabajadorPropio(40L, null)).isFalse();
        assertThat(ownership.jornalPropio(50L, null)).isFalse();
        assertThat(ownership.nominaPropia(60L, null)).isFalse();
        assertThat(ownership.pagoPropio(70L, null)).isFalse();
    }

    // ── labores de los jornales ─────────────────────────────────────────────
    @Test
    @DisplayName("laborPropia: el agricultor modifica solo sus labores, nunca las comunes")
    void laborPropia() {
        com.agroruta.worker.domain.Labor comun = new com.agroruta.worker.domain.Labor(1L, "Recolección", "");
        com.agroruta.worker.domain.Labor propia = new com.agroruta.worker.domain.Labor(2L, "Deshoje", "");
        propia.setAgricultorId(1L);
        org.mockito.Mockito.when(laborRepository.buscarPorId(1L)).thenReturn(java.util.Optional.of(comun));
        org.mockito.Mockito.when(laborRepository.buscarPorId(2L)).thenReturn(java.util.Optional.of(propia));

        assertThat(ownership.laborPropia(2L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.laborPropia(2L, auth(3L, Rol.Agricultor))).isFalse();
        assertThat(ownership.laborPropia(1L, auth(1L, Rol.Agricultor))).isFalse();
        assertThat(ownership.laborPropia(1L, auth(9L, Rol.Administrador))).isTrue();
    }
}
