package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.out.DiagnosticoIaPort;
import com.agroruta.crop.application.ports.out.DiagnosticoPlagaRepository;
import com.agroruta.crop.application.ports.out.SiembraRepository;
import com.agroruta.crop.domain.AnalisisFoto;
import com.agroruta.crop.domain.ConfianzaDiagnostico;
import com.agroruta.crop.domain.ContextoCultivo;
import com.agroruta.crop.domain.DiagnosticoPlaga;
import com.agroruta.crop.domain.DiagnosticosSiembra;
import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.crop.domain.TipoProblema;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ExternalApiException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosticoPlagaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
    private static final Long SIEMBRA = 7L;
    private static final Long USUARIO = 3L;

    @Mock private DiagnosticoPlagaRepository repositorio;
    @Mock private DiagnosticoIaPort ia;
    @Mock private SiembraRepository siembras;

    private DiagnosticoPlagaService service;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-11T15:00:00Z"), ZONA);
        service = new DiagnosticoPlagaService(repositorio, ia, siembras, 3, 100, reloj);
        lenient().when(ia.disponible()).thenReturn(true);

        Siembra siembra = new Siembra();
        siembra.setId(SIEMBRA);
        siembra.setTipoCultivo("Café");
        siembra.setVariedad("Castillo");
        siembra.setEstadoCultivo(EstadoCultivo.CRECIMIENTO);
        siembra.setFechaSiembra(HOY.minusDays(40));
        lenient().when(siembras.findById(SIEMBRA)).thenReturn(Optional.of(siembra));
        lenient().when(repositorio.guardar(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            AnalisisFoto a = inv.getArgument(5);
            return new DiagnosticoPlaga(1L, inv.getArgument(0), inv.getArgument(2), inv.getArgument(4), a.problema(), a.tipo(),
                    a.confianza(), a.sintomas(), a.recomendaciones(), a.otrasPosibilidades(), a.advertencia());
        });
    }

    private static byte[] foto(int ancho, int alto, String formato) {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < ancho; x += 7) imagen.setRGB(x, alto / 2, 0x2E7D32);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            ImageIO.write(imagen, formato, salida);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return salida.toByteArray();
    }

    private static AnalisisFoto roya() {
        return new AnalisisFoto(true, "Roya del café (Hemileia vastatrix)", TipoProblema.ENFERMEDAD, ConfianzaDiagnostico.MEDIA,
                "Polvillo  anaranjado\nen el envés.", List.of("Revisa el resto del lote.", "  ", "Retira las hojas más afectadas."),
                List.of("Mancha de hierro"), "", "claude-opus-5-5");
    }

    @Test
    void analizaLaFotoReducidaYGuardaElResultadoEnLaBitacora() throws IOException {
        when(ia.analizar(any(), any())).thenReturn(Optional.of(roya()));

        DiagnosticoPlaga d = service.diagnosticar(SIEMBRA, USUARIO, foto(3000, 2000, "jpg"), "  Hojas con\n polvo naranja ");

        ArgumentCaptor<byte[]> enviada = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<ContextoCultivo> contexto = ArgumentCaptor.forClass(ContextoCultivo.class);
        verify(ia).analizar(enviada.capture(), contexto.capture());
        BufferedImage analizada = ImageIO.read(new ByteArrayInputStream(enviada.getValue()));
        assertEquals(1280, analizada.getWidth());
        assertEquals(853, analizada.getHeight());
        assertEquals(new ContextoCultivo("Café", "Castillo", "crecimiento", 40L, "Hojas con polvo naranja"), contexto.getValue());

        ArgumentCaptor<byte[]> guardada = ArgumentCaptor.forClass(byte[].class);
        verify(repositorio).guardar(eq(SIEMBRA), eq(USUARIO), eq(LocalDateTime.of(2026, 11, 11, 10, 0)), guardada.capture(),
                eq("Hojas con polvo naranja"), any());
        assertEquals(640, ImageIO.read(new ByteArrayInputStream(guardada.getValue())).getWidth());
        verify(repositorio).anotarUso(USUARIO, HOY);

        assertEquals("Roya del café (Hemileia vastatrix)", d.problema());
        assertEquals("Polvillo anaranjado en el envés.", d.sintomas());
        assertEquals(List.of("Revisa el resto del lote.", "Retira las hojas más afectadas."), d.recomendaciones());
        assertEquals(null, d.advertencia());
    }

    @Test
    void unaFotoPequenaNoSeAgranda() throws IOException {
        when(ia.analizar(any(), any())).thenReturn(Optional.of(roya()));
        service.diagnosticar(SIEMBRA, USUARIO, foto(400, 300, "png"), null);
        ArgumentCaptor<byte[]> enviada = ArgumentCaptor.forClass(byte[].class);
        verify(ia).analizar(enviada.capture(), any());
        assertEquals(400, ImageIO.read(new ByteArrayInputStream(enviada.getValue())).getWidth());
    }

    @Test
    void sinClaveDelServicioNoSeIntenta() {
        when(ia.disponible()).thenReturn(false);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.diagnosticar(SIEMBRA, USUARIO, foto(400, 300, "jpg"), null));
        assertEquals(ErrorCode.INVALID_OPERATION, e.getErrorCode());
        verify(ia, never()).analizar(any(), any());
    }

    @Test
    void rechazaArchivosQueNoSonFotosSinGastarUnAnalisis() {
        for (byte[] archivo : List.of(new byte[0], "no soy una foto".getBytes(), foto(50, 50, "png"), foto(400, 300, "bmp"))) {
            BusinessException e = assertThrows(BusinessException.class, () -> service.diagnosticar(SIEMBRA, USUARIO, archivo, null));
            assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
        verify(ia, never()).analizar(any(), any());
        verify(repositorio, never()).anotarUso(any(), any());
    }

    @Test
    void laSiembraEliminadaNoRecibeDiagnosticos() {
        Siembra eliminada = new Siembra();
        eliminada.setEliminadoEn(LocalDateTime.now());
        when(siembras.findById(SIEMBRA)).thenReturn(Optional.of(eliminada));
        assertThrows(ResourceNotFoundException.class, () -> service.diagnosticar(SIEMBRA, USUARIO, foto(400, 300, "jpg"), null));
    }

    @Test
    void respetaElTopeDiarioDeLaPersonaYElGeneral() {
        when(repositorio.usoDe(USUARIO, HOY)).thenReturn(3);
        BusinessException propio = assertThrows(BusinessException.class,
                () -> service.diagnosticar(SIEMBRA, USUARIO, foto(400, 300, "jpg"), null));
        assertEquals(ErrorCode.TOO_MANY_ATTEMPTS, propio.getErrorCode());

        when(repositorio.usoDe(USUARIO, HOY)).thenReturn(0);
        when(repositorio.usoTotal(HOY)).thenReturn(100);
        BusinessException general = assertThrows(BusinessException.class,
                () -> service.diagnosticar(SIEMBRA, USUARIO, foto(400, 300, "jpg"), null));
        assertEquals(ErrorCode.TOO_MANY_ATTEMPTS, general.getErrorCode());
        verify(ia, never()).analizar(any(), any());
    }

    @Test
    void siElServicioFallaSeDevuelveElUso() {
        when(ia.analizar(any(), any())).thenThrow(new ExternalApiException("caído"));
        assertThrows(ExternalApiException.class, () -> service.diagnosticar(SIEMBRA, USUARIO, foto(400, 300, "jpg"), null));
        verify(repositorio).anotarUso(USUARIO, HOY);
        verify(repositorio).devolverUso(USUARIO, HOY);
        verify(repositorio, never()).guardar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void unaFotoSinPlantaNoSeGuardaPeroCuenta() {
        when(ia.analizar(any(), any())).thenReturn(Optional.of(new AnalisisFoto(false, "Se ve una herramienta, no una planta.",
                null, null, null, null, null, null, "claude-opus-5-5")));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.diagnosticar(SIEMBRA, USUARIO, foto(400, 300, "jpg"), null));
        assertEquals(ErrorCode.INVALID_OPERATION, e.getErrorCode());
        assertTrue(e.getMessage().contains("Se ve una herramienta"));
        verify(repositorio, never()).guardar(any(), any(), any(), any(), any(), any());
        verify(repositorio, never()).devolverUso(any(), any());
    }

    @Test
    void siElModeloDeclinaLoDiceSinGuardar() {
        when(ia.analizar(any(), any())).thenReturn(Optional.empty());
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.diagnosticar(SIEMBRA, USUARIO, foto(400, 300, "jpg"), null));
        assertEquals(ErrorCode.INVALID_OPERATION, e.getErrorCode());
        verify(repositorio, never()).guardar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void unaRespuestaIncompletaSeGuardaComoIndeterminada() {
        when(ia.analizar(any(), any())).thenReturn(Optional.of(new AnalisisFoto(true, " ", null, null, null, null, null, null, null)));
        DiagnosticoPlaga d = service.diagnosticar(SIEMBRA, USUARIO, foto(400, 300, "jpg"), null);
        assertEquals("No se pudo determinar", d.problema());
        assertEquals(TipoProblema.INDETERMINADO, d.tipo());
        assertEquals(ConfianzaDiagnostico.BAJA, d.confianza());
        assertTrue(d.recomendaciones().isEmpty());
    }

    @Test
    void laListaDiceSiHayServicioYCuantasFotosQuedanHoy() {
        when(repositorio.usoDe(USUARIO, HOY)).thenReturn(2);
        when(repositorio.deSiembra(eq(SIEMBRA), anyInt())).thenReturn(List.of());
        DiagnosticosSiembra lista = service.deSiembra(SIEMBRA, USUARIO);
        assertTrue(lista.disponible());
        assertEquals(1, lista.restantesHoy());

        when(ia.disponible()).thenReturn(false);
        when(repositorio.usoDe(USUARIO, HOY)).thenReturn(9);
        DiagnosticosSiembra apagado = service.deSiembra(SIEMBRA, USUARIO);
        assertFalse(apagado.disponible());
        assertEquals(0, apagado.restantesHoy());
    }

    @Test
    void laFotoYElBorradoSoloValenDentroDeSuSiembra() {
        when(repositorio.foto(SIEMBRA, 9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.foto(SIEMBRA, 9L));
        when(repositorio.eliminar(SIEMBRA, 9L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(SIEMBRA, 9L));
        when(repositorio.eliminar(SIEMBRA, 4L)).thenReturn(true);
        service.eliminar(SIEMBRA, 4L);
    }
}
