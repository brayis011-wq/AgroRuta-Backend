package com.agroruta.finance.infrastructure.web;

import com.agroruta.finance.application.ports.in.VentasUseCase;
import com.agroruta.finance.domain.ResumenVentas;
import com.agroruta.finance.domain.Venta;
import com.agroruta.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Ventas y cuentas por cobrar. */
@RestController
@RequestMapping("/api/sales")
public class VentaController {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final VentasUseCase ventas;

    public VentaController(VentasUseCase ventas) {
        this.ventas = ventas;
    }

    public record VentaRequest(
            Long siembraId,
            @NotBlank(message = "Escribe a quién le vendiste") @Size(max = 120, message = "Máximo 120 caracteres") String comprador,
            @NotNull(message = "Indica la fecha") @PastOrPresent(message = "La fecha no puede ser futura") LocalDate fecha,
            @NotBlank(message = "Escribe qué vendiste") @Size(max = 120, message = "Máximo 120 caracteres") String producto,
            @Positive(message = "Los kilos deben ser más de 0") @DecimalMax(value = "10000000000") @Digits(integer = 10, fraction = 2) BigDecimal cantidadKg,
            @Positive(message = "El precio debe ser mayor a 0") @DecimalMax(value = "100000000") @Digits(integer = 12, fraction = 2) BigDecimal precioKg,
            @Positive(message = "El total debe ser mayor a 0") @DecimalMax(value = "100000000000") @Digits(integer = 12, fraction = 2) BigDecimal total,
            LocalDate vence,
            @Size(max = 255, message = "Máximo 255 caracteres") String observaciones,
            /** Solo al crear: ya te pagaron todo */
            Boolean pagadoDeContado,
            String metodo) {
        VentasUseCase.DatosVenta datos() {
            return new VentasUseCase.DatosVenta(siembraId, comprador, fecha, producto, cantidadKg, precioKg, total, vence, observaciones);
        }
    }

    public record AbonoRequest(
            @NotNull(message = "Indica la fecha") @PastOrPresent(message = "La fecha no puede ser futura") LocalDate fecha,
            @NotNull(message = "Indica el valor") @Positive(message = "El valor debe ser mayor a 0")
            @DecimalMax(value = "100000000000") @Digits(integer = 12, fraction = 2) BigDecimal monto,
            String metodo,
            @Size(max = 255, message = "Máximo 255 caracteres") String observaciones) {}

    /** Lo que se devuelve: la venta con su saldo y su estado de hoy */
    public record VentaResponse(Long id, Long siembraId, String siembraNombre, String comprador, LocalDate fecha,
                                String producto, BigDecimal cantidadKg, BigDecimal precioKg, BigDecimal total,
                                LocalDate vence, String observaciones, BigDecimal abonado, BigDecimal saldo,
                                Venta.Estado estado, List<Venta.Abono> abonos) {
        static VentaResponse from(Venta v) {
            return new VentaResponse(v.id(), v.siembraId(), v.siembraNombre(), v.comprador(), v.fecha(), v.producto(),
                    v.cantidadKg(), v.precioKg(), v.total(), v.vence(), v.observaciones(), v.abonado(), v.saldo(),
                    v.estado(LocalDate.now(ZONA)), v.abonos());
        }
    }

    /** ?status=PENDIENTE|VENCIDA|PAGADA&buyer=juan&desde=&hasta= (todo opcional) */
    @GetMapping
    public ResponseEntity<List<VentaResponse>> listar(
            @RequestParam(value = "status", required = false) Venta.Estado estado,
            @RequestParam(value = "buyer", required = false) String comprador,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {
        return ResponseEntity.ok(ventas.listar(dueno(authentication), estado, comprador, desde, hasta)
                .stream().map(VentaResponse::from).toList());
    }

    @GetMapping("/summary")
    public ResponseEntity<ResumenVentas> resumen(Authentication authentication) {
        return ResponseEntity.ok(ventas.resumen(dueno(authentication)));
    }

    @GetMapping("/buyers")
    public ResponseEntity<List<String>> compradores(Authentication authentication) {
        return ResponseEntity.ok(ventas.compradores(CurrentUser.id(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.ventaPropia(#id, authentication)")
    public ResponseEntity<VentaResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(VentaResponse.from(ventas.buscarPorId(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('Agricultor') and (#req.siembraId == null or @ownership.siembraPropia(#req.siembraId, authentication))")
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest req, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(VentaResponse.from(
                ventas.registrar(CurrentUser.id(authentication), req.datos(), Boolean.TRUE.equals(req.pagadoDeContado()), req.metodo())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ownership.ventaPropia(#id, authentication) and (#req.siembraId == null or @ownership.siembraPropia(#req.siembraId, authentication))")
    public ResponseEntity<VentaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody VentaRequest req) {
        return ResponseEntity.ok(VentaResponse.from(ventas.actualizar(id, req.datos())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.ventaPropia(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ventas.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Un pago (abono) del comprador */
    @PostMapping("/{id}/payments")
    @PreAuthorize("@ownership.ventaPropia(#id, authentication)")
    public ResponseEntity<VentaResponse> abonar(@PathVariable Long id, @Valid @RequestBody AbonoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(VentaResponse.from(
                ventas.abonar(id, req.fecha(), req.monto(), req.metodo(), req.observaciones())));
    }

    @DeleteMapping("/{id}/payments/{abonoId}")
    @PreAuthorize("@ownership.ventaPropia(#id, authentication)")
    public ResponseEntity<VentaResponse> quitarAbono(@PathVariable Long id, @PathVariable Long abonoId) {
        return ResponseEntity.ok(VentaResponse.from(ventas.quitarAbono(id, abonoId)));
    }

    private static Long dueno(Authentication authentication) {
        return CurrentUser.esAdmin(authentication) ? null : CurrentUser.id(authentication);
    }
}
