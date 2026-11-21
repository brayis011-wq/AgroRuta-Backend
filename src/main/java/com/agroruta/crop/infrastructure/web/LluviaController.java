package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.LluviaService;
import com.agroruta.crop.domain.Lluvia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lluvia por finca y día (lo que marcó el pluviómetro), para decidir riegos y fumigaciones. */
@RestController
@RequestMapping("/api/farms/{fincaId}/rain")
public class LluviaController {

    private final LluviaService lluvias;

    public LluviaController(LluviaService lluvias) {
        this.lluvias = lluvias;
    }

    public record LluviaRequest(
            @NotNull(message = "Indica los milímetros") @PositiveOrZero(message = "No pueden ser negativos")
            @DecimalMax(value = "500", message = "Son demasiados milímetros para un día") BigDecimal milimetros) {}

    /** ?desde=2026-11-01&hasta=2026-11-21 (por defecto, los últimos 30 días) */
    @GetMapping
    @PreAuthorize("@ownership.fincaPropia(#fincaId, authentication)")
    public ResponseEntity<Lluvia.Resumen> listar(@PathVariable Long fincaId,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(lluvias.deFinca(fincaId, desde, hasta));
    }

    /** Anota (o corrige) la lluvia de un día; 0 = ese día no llovió */
    @PutMapping("/{fecha}")
    @PreAuthorize("@ownership.fincaPropia(#fincaId, authentication)")
    public ResponseEntity<Lluvia> anotar(@PathVariable Long fincaId,
                                         @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                         @Valid @RequestBody LluviaRequest req) {
        return ResponseEntity.ok(lluvias.anotar(fincaId, fecha, req.milimetros()));
    }

    @DeleteMapping("/{fecha}")
    @PreAuthorize("@ownership.fincaPropia(#fincaId, authentication)")
    public ResponseEntity<Void> borrar(@PathVariable Long fincaId,
                                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        lluvias.borrar(fincaId, fecha);
        return ResponseEntity.noContent().build();
    }
}
