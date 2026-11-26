package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.ActividadProgramadaUseCase;
import com.agroruta.crop.domain.ActividadProgramada;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Labores que se repiten en una siembra ("fertilizar cada 30 días"). */
@RestController
@RequestMapping("/api")
public class ActividadProgramadaController {

    private final ActividadProgramadaUseCase programadas;

    public ActividadProgramadaController(ActividadProgramadaUseCase programadas) {
        this.programadas = programadas;
    }

    public record ProgramadaRequest(
            @NotBlank(message = "Elige la labor") @Size(max = 20)
            String tipo,

            @Size(max = 255, message = "Máximo 255 caracteres")
            String descripcion,

            @NotNull(message = "Indica cada cuántos días")
            @Min(value = 1, message = "Mínimo cada día")
            @Max(value = 365, message = "Máximo una vez al año")
            Integer cadaDias,

            @NotNull(message = "Indica la primera vez") LocalDate desde,
            LocalDate hasta) {}

    /** Lo que se devuelve: la programación con su próxima fecha */
    public record ProgramadaResponse(
            Long id,
            Long siembraId,
            String tipo,
            String descripcion,
            int cadaDias,
            LocalDate desde,
            LocalDate hasta,
            LocalDate proxima,
            LocalDate ultimaVez,
            boolean atrasada,
            long diasAtraso) {
        static ProgramadaResponse from(ActividadProgramadaUseCase.ConEstado c) {
            ActividadProgramada p = c.programada();
            var e = c.estado();
            return new ProgramadaResponse(
                    p.id(),
                    p.siembraId(),
                    p.tipo(),
                    p.descripcion(),
                    p.cadaDias(),
                    p.desde(),
                    p.hasta(),
                    e == null ? null : e.proxima(),
                    e == null ? null : e.ultimaVez(),
                    e != null && e.atrasada(),
                    e == null ? 0 : e.diasAtraso());
        }
    }

    @GetMapping("/plantings/{siembraId}/recurring")
    @PreAuthorize("@ownership.siembraPropia(#siembraId, authentication)")
    public ResponseEntity<List<ProgramadaResponse>> listar(@PathVariable Long siembraId) {
        return ResponseEntity.ok(programadas.deSiembra(siembraId).stream()
                .map(ProgramadaResponse::from)
                .toList());
    }

    @PostMapping("/plantings/{siembraId}/recurring")
    @PreAuthorize("@ownership.siembraPropia(#siembraId, authentication)")
    public ResponseEntity<ProgramadaResponse> crear(
            @PathVariable Long siembraId, @Valid @RequestBody ProgramadaRequest req) {
        ActividadProgramada p =
                programadas.crear(siembraId, req.tipo(), req.descripcion(), req.cadaDias(), req.desde(), req.hasta());
        return ResponseEntity.status(HttpStatus.CREATED).body(conEstado(siembraId, p.id()));
    }

    @PutMapping("/recurring-activities/{id}")
    @PreAuthorize("@ownership.programadaPropia(#id, authentication)")
    public ResponseEntity<ProgramadaResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ProgramadaRequest req) {
        ActividadProgramada p =
                programadas.actualizar(id, req.tipo(), req.descripcion(), req.cadaDias(), req.desde(), req.hasta());
        return ResponseEntity.ok(conEstado(p.siembraId(), id));
    }

    @DeleteMapping("/recurring-activities/{id}")
    @PreAuthorize("@ownership.programadaPropia(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        programadas.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private ProgramadaResponse conEstado(Long siembraId, Long id) {
        return programadas.deSiembra(siembraId).stream()
                .filter(c -> id.equals(c.programada().id()))
                .findFirst()
                .map(ProgramadaResponse::from)
                .orElseThrow();
    }
}
