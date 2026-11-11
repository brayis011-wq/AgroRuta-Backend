package com.agroruta.worker.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.out.ComprobantePdfPort;
import com.agroruta.worker.application.ports.out.EmpleadorPort;
import com.agroruta.worker.application.ports.out.PagoRepository;
import com.agroruta.worker.application.ports.out.TrabajadorRepository;
import com.agroruta.worker.domain.ComprobantePago;
import com.agroruta.worker.domain.EstadoAccesoPortal;
import com.agroruta.worker.domain.MetodoPago;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.domain.Trabajador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComprobantePagoService")
class ComprobantePagoServiceTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private TrabajadorRepository trabajadorRepository;
    @Mock private EmpleadorPort empleadorPort;
    @Mock private ComprobantePdfPort pdfPort;

    private ComprobantePagoService service;
    private Trabajador trabajador;
    private Pago pago;

    @BeforeEach
    void preparar() {
        service = new ComprobantePagoService(pagoRepository, trabajadorRepository, empleadorPort, pdfPort,
                "c2VjcmV0by1kZS1wcnVlYmEtcGFyYS1sb3MtdGVzdHM=", "https://app.ejemplo.test/");
        trabajador = new Trabajador();
        trabajador.setId(4L);
        trabajador.setNombre("Juan");
        trabajador.setApellido("Pérez");
        trabajador.setCedula("1052334120");
        trabajador.setAgricultorId(9L);
        trabajador.setUsuarioId(77L);
        trabajador.setAccesoEstado(EstadoAccesoPortal.APROBADO);
        Nomina nomina = new Nomina(3L, trabajador, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15), List.of());
        pago = new Pago(12L, nomina, LocalDate.of(2026, 10, 16), new BigDecimal("450000.00"), MetodoPago.EFECTIVO, "REC-1");
    }

    @Test
    @DisplayName("arma el comprobante con número, empleador y un enlace de verificación")
    void armaElComprobante() {
        when(pagoRepository.buscarPorId(12L)).thenReturn(Optional.of(pago));
        when(empleadorPort.nombre(9L)).thenReturn(Optional.of("Finca La Esperanza"));
        when(pdfPort.generar(any())).thenReturn(new byte[]{1});

        assertArrayEquals(new byte[]{1}, service.pdf(12L));

        ArgumentCaptor<ComprobantePago> c = ArgumentCaptor.forClass(ComprobantePago.class);
        verify(pdfPort).generar(c.capture());
        assertEquals("CP-000012", c.getValue().numero());
        assertEquals("Juan Pérez", c.getValue().trabajador());
        assertEquals("Finca La Esperanza", c.getValue().empleador());
        assertEquals(16, c.getValue().codigo().length());
        assertEquals("https://app.ejemplo.test/receipt/12/" + c.getValue().codigo(), c.getValue().urlVerificacion());
    }

    @Test
    @DisplayName("verifica con el código correcto y rechaza cualquier otro")
    void verificaElCodigo() {
        when(pagoRepository.buscarPorId(12L)).thenReturn(Optional.of(pago));
        String codigo = service.codigo(pago);

        assertTrue(service.verificar(12L, codigo).isPresent());
        assertTrue(service.verificar(12L, " " + codigo + " ").isPresent());
        assertTrue(service.verificar(12L, "AAAAAAAAAAAAAAAA").isEmpty());
        assertTrue(service.verificar(12L, "").isEmpty());
        assertTrue(service.verificar(12L, null).isEmpty());
    }

    @Test
    @DisplayName("el código cambia si cambia el valor del pago")
    void elCodigoDependeDelValor() {
        String original = service.codigo(pago);
        pago.setMonto(new BigDecimal("950000"));
        assertNotEquals(original, service.codigo(pago));
    }

    @Test
    @DisplayName("el código no depende de los ceros decimales del valor")
    void elCodigoIgnoraCerosDecimales() {
        String original = service.codigo(pago);
        pago.setMonto(new BigDecimal("450000"));
        assertEquals(original, service.codigo(pago));
    }

    @Test
    @DisplayName("un pago que no existe no se verifica")
    void pagoInexistente() {
        when(pagoRepository.buscarPorId(99L)).thenReturn(Optional.empty());
        assertTrue(service.verificar(99L, "AAAAAAAAAAAAAAAA").isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.pdf(99L));
    }

    @Test
    @DisplayName("el trabajador descarga el comprobante de su propio pago")
    void trabajadorDescargaElSuyo() {
        when(trabajadorRepository.buscarPorUsuarioId(77L)).thenReturn(Optional.of(trabajador));
        when(pagoRepository.buscarPorId(12L)).thenReturn(Optional.of(pago));
        when(pdfPort.generar(any())).thenReturn(new byte[]{7});

        assertArrayEquals(new byte[]{7}, service.pdfDelTrabajador(77L, 12L));
    }

    @Test
    @DisplayName("el trabajador no puede descargar el comprobante de otro trabajador")
    void trabajadorNoVeElDeOtro() {
        Trabajador otro = new Trabajador();
        otro.setId(5L);
        otro.setUsuarioId(88L);
        otro.setAccesoEstado(EstadoAccesoPortal.APROBADO);
        when(trabajadorRepository.buscarPorUsuarioId(88L)).thenReturn(Optional.of(otro));
        when(pagoRepository.buscarPorId(12L)).thenReturn(Optional.of(pago));

        assertThrows(ResourceNotFoundException.class, () -> service.pdfDelTrabajador(88L, 12L));
        verify(pdfPort, never()).generar(any());
    }

    @Test
    @DisplayName("sin acceso aprobado no hay comprobante")
    void sinAccesoAprobado() {
        trabajador.setAccesoEstado(EstadoAccesoPortal.PENDIENTE);
        when(trabajadorRepository.buscarPorUsuarioId(77L)).thenReturn(Optional.of(trabajador));

        assertThrows(BusinessException.class, () -> service.pdfDelTrabajador(77L, 12L));
        verify(pagoRepository, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("la cédula se oculta para quien solo verifica")
    void cedulaOculta() {
        when(pagoRepository.buscarPorId(12L)).thenReturn(Optional.of(pago));
        ComprobantePago c = service.verificar(12L, service.codigo(pago)).orElseThrow();
        assertEquals("•••• 120", c.cedulaOculta());
    }
}
