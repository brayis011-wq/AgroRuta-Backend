package com.agroruta.finance.infrastructure.web;

import com.agroruta.finance.application.ports.in.GastosFincaUseCase;
import com.agroruta.finance.domain.GastoFinca;
import com.agroruta.finance.domain.ResumenGastos;
import com.agroruta.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Gastos generales de la finca: transporte, herramientas, servicios… (no son de una siembra). */
@RestController
@RequestMapping("/api/farm-expenses")
public class GastoFincaController {

    private final GastosFincaUseCase gastos;

    public GastoFincaController(GastosFincaUseCase gastos) {
        this.gastos = gastos;
    }

    public record GastoRequest(
            @NotNull(message = "Elige la finca") Long fincaId,

            @NotNull(message = "Indica la fecha") @PastOrPresent(message = "La fecha no puede ser futura")
            LocalDate fecha,

            GastoFinca.Categoria categoria,

            @NotBlank(message = "Escribe en qué se gastó") @Size(max = 255, message = "Máximo 255 caracteres")
            String descripcion,

            @NotNull(message = "Indica el valor")
            @Positive(message = "El valor debe ser mayor a 0")
            @DecimalMax(value = "100000000000", message = "El valor es demasiado alto")
            @Digits(integer = 12, fraction = 2, message = "El valor admite máximo 2 decimales")
            BigDecimal monto) {}

    /** ?farmId=3&desde=2026-01-01&hasta=2026-12-31 (todo opcional) */
    @GetMapping
    @PreAuthorize("#fincaId == null or @ownership.fincaPropia(#fincaId, authentication)")
    public ResponseEntity<List<GastoFinca>> listar(
            @RequestParam(value = "farmId", required = false) Long fincaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {
        return ResponseEntity.ok(gastos.listar(dueno(authentication), fincaId, desde, hasta));
    }

    /** Total por categoría y por finca en el periodo (para la rentabilidad) */
    @GetMapping("/summary")
    public ResponseEntity<ResumenGastos> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {
        return ResponseEntity.ok(gastos.resumen(dueno(authentication), desde, hasta));
    }

    @PostMapping
    @PreAuthorize("@ownership.fincaPropia(#req.fincaId, authentication)")
    public ResponseEntity<GastoFinca> registrar(@Valid @RequestBody GastoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gastos.registrar(req.fincaId(), req.fecha(), req.categoria(), req.descripcion(), req.monto()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "@ownership.gastoPropio(#id, authentication) and @ownership.fincaPropia(#req.fincaId, authentication)")
    public ResponseEntity<GastoFinca> actualizar(@PathVariable Long id, @Valid @RequestBody GastoRequest req) {
        return ResponseEntity.ok(
                gastos.actualizar(id, req.fincaId(), req.fecha(), req.categoria(), req.descripcion(), req.monto()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.gastoPropio(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        gastos.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private static Long dueno(Authentication authentication) {
        return CurrentUser.esAdmin(authentication) ? null : CurrentUser.id(authentication);
    }
}
