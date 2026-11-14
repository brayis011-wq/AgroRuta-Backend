package com.agroruta.shared.security;

import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.infrastructure.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OwnershipChecker")
class OwnershipCheckerTest {

    @Mock private ConsultaPropiedad consulta;

    @InjectMocks private OwnershipChecker ownership;

    private Authentication auth(Long id, Rol rol) {
        CustomUserDetails d = new CustomUserDetails(new Usuario(id, "Nombre", "a@a.com", "pw", rol));
        return new UsernamePasswordAuthenticationToken(d, null, d.getAuthorities());
    }

    @Test
    @DisplayName("esAgricultor: true si el id coincide, false si no")
    void esAgricultor() {
        assertThat(ownership.esAgricultor(1L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.esAgricultor(2L, auth(1L, Rol.Agricultor))).isFalse();
        assertThat(ownership.esAgricultor(null, auth(1L, Rol.Agricultor))).isFalse();
        verifyNoInteractions(consulta);
    }

    @Test
    @DisplayName("fincaPropia: dueño sí, otro usuario no")
    void fincaPropia() {
        when(consulta.esDueno(Recurso.FINCA, 10L, 1L)).thenReturn(true);

        assertThat(ownership.fincaPropia(10L, auth(1L, Rol.Agricultor))).isTrue();
        assertThat(ownership.fincaPropia(10L, auth(2L, Rol.Agricultor))).isFalse();
    }

    @Test
    @DisplayName("fincaPropia: finca inexistente devuelve false")
    void fincaInexistente() {
        assertThat(ownership.fincaPropia(999L, auth(1L, Rol.Agricultor))).isFalse();
        verify(consulta).esDueno(Recurso.FINCA, 999L, 1L);
    }

    @Test
    @DisplayName("Cada verificación pregunta por su tipo de recurso, con el id del usuario")
    void cadaMetodoConsultaSuRecurso() {
        Authentication dueno = auth(1L, Rol.Agricultor);
        Authentication otro = auth(2L, Rol.Agricultor);
        for (Recurso recurso : Recurso.values()) {
            when(consulta.esDueno(recurso, 7L, 1L)).thenReturn(true);
        }

        assertThat(ownership.fincaPropia(7L, dueno)).isTrue();
        assertThat(ownership.lotePropio(7L, dueno)).isTrue();
        assertThat(ownership.siembraPropia(7L, dueno)).isTrue();
        assertThat(ownership.cosechaPropia(7L, dueno)).isTrue();
        assertThat(ownership.fumigacionPropia(7L, dueno)).isTrue();
        assertThat(ownership.actividadPropia(7L, dueno)).isTrue();
        assertThat(ownership.laborPropia(7L, dueno)).isTrue();
        assertThat(ownership.trabajadorPropio(7L, dueno)).isTrue();
        assertThat(ownership.cargoPropio(7L, dueno)).isTrue();
        assertThat(ownership.jornalPropio(7L, dueno)).isTrue();
        assertThat(ownership.nominaPropia(7L, dueno)).isTrue();
        assertThat(ownership.pagoPropio(7L, dueno)).isTrue();

        assertThat(ownership.fincaPropia(7L, otro)).isFalse();
        assertThat(ownership.lotePropio(7L, otro)).isFalse();
        assertThat(ownership.siembraPropia(7L, otro)).isFalse();
        assertThat(ownership.cosechaPropia(7L, otro)).isFalse();
        assertThat(ownership.fumigacionPropia(7L, otro)).isFalse();
        assertThat(ownership.actividadPropia(7L, otro)).isFalse();
        assertThat(ownership.laborPropia(7L, otro)).isFalse();
        assertThat(ownership.trabajadorPropio(7L, otro)).isFalse();
        assertThat(ownership.cargoPropio(7L, otro)).isFalse();
        assertThat(ownership.jornalPropio(7L, otro)).isFalse();
        assertThat(ownership.nominaPropia(7L, otro)).isFalse();
        assertThat(ownership.pagoPropio(7L, otro)).isFalse();

        for (Recurso recurso : Recurso.values()) {
            verify(consulta).esDueno(recurso, 7L, 1L);
            verify(consulta).esDueno(recurso, 7L, 2L);
        }
    }

    @Test
    @DisplayName("Administrador accede a todo sin consultar la base")
    void administradorAccedeATodo() {
        Authentication admin = auth(99L, Rol.Administrador);

        assertThat(ownership.esAgricultor(1L, admin)).isTrue();
        assertThat(ownership.fincaPropia(10L, admin)).isTrue();
        assertThat(ownership.lotePropio(20L, admin)).isTrue();
        assertThat(ownership.siembraPropia(30L, admin)).isTrue();
        assertThat(ownership.cosechaPropia(35L, admin)).isTrue();
        assertThat(ownership.fumigacionPropia(36L, admin)).isTrue();
        assertThat(ownership.actividadPropia(37L, admin)).isTrue();
        assertThat(ownership.laborPropia(38L, admin)).isTrue();
        assertThat(ownership.trabajadorPropio(40L, admin)).isTrue();
        assertThat(ownership.cargoPropio(45L, admin)).isTrue();
        assertThat(ownership.jornalPropio(50L, admin)).isTrue();
        assertThat(ownership.nominaPropia(60L, admin)).isTrue();
        assertThat(ownership.pagoPropio(70L, admin)).isTrue();
        verifyNoInteractions(consulta);
    }

    @Test
    @DisplayName("Sin autenticación o sin id todo es false, sin consultar la base")
    void sinAutenticacion() {
        assertThat(ownership.esAgricultor(1L, null)).isFalse();
        assertThat(ownership.fincaPropia(10L, null)).isFalse();
        assertThat(ownership.lotePropio(20L, null)).isFalse();
        assertThat(ownership.siembraPropia(30L, null)).isFalse();
        assertThat(ownership.trabajadorPropio(40L, null)).isFalse();
        assertThat(ownership.jornalPropio(50L, null)).isFalse();
        assertThat(ownership.nominaPropia(60L, null)).isFalse();
        assertThat(ownership.pagoPropio(70L, null)).isFalse();
        assertThat(ownership.fincaPropia(null, auth(1L, Rol.Agricultor))).isFalse();
        assertThat(ownership.pagoPropio(null, auth(1L, Rol.Agricultor))).isFalse();
        verifyNoInteractions(consulta);
    }
}
