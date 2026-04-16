package com.agroruta.worker.infrastructure.web;

import com.agroruta.worker.application.ports.in.JornalUseCase;
import com.agroruta.worker.infrastructure.web.dto.JornalRequest;
import com.agroruta.worker.infrastructure.web.dto.JornalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/jornales")
public class JornalController {

    private final JornalUseCase jornalUseCase;

    public JornalController(JornalUseCase jornalUseCase) {
        this.jornalUseCase = jornalUseCase;
    }

    @PostMapping
    public ResponseEntity<JornalResponse> registrar(@RequestBody JornalRequest req) {
        JornalResponse response = JornalResponse.from(
                jornalUseCase.registrarJornal(
                        req.trabajadorId, req.cultivoId, req.nombreCultivo,
                        req.fecha, req.actividadIds, req.observaciones
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JornalResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(JornalResponse.from(jornalUseCase.buscarPorId(id)));
    }

    @GetMapping("/trabajador/{trabajadorId}")
    public ResponseEntity<List<JornalResponse>> porTrabajador(
            @PathVariable Long trabajadorId,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fin) {

        List<JornalResponse> jornales = (inicio != null && fin != null)
                ? jornalUseCase.listarPorTrabajadorYPeriodo(trabajadorId, inicio, fin)
                .stream().map(JornalResponse::from).toList()
                : jornalUseCase.listarPorTrabajador(trabajadorId)
                .stream().map(JornalResponse::from).toList();

        return ResponseEntity.ok(jornales);
    }

    @GetMapping("/cultivo/{cultivoId}")
    public ResponseEntity<List<JornalResponse>> porCultivo(
            @PathVariable Long cultivoId,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fin) {

        List<JornalResponse> jornales = (inicio != null && fin != null)
                ? jornalUseCase.listarPorCultivoYPeriodo(cultivoId, inicio, fin)
                .stream().map(JornalResponse::from).toList()
                : jornalUseCase.listarPorCultivo(cultivoId)
                .stream().map(JornalResponse::from).toList();

        return ResponseEntity.ok(jornales);
    }

    @PatchMapping("/{id}/actividades/{actividadId}")
    public ResponseEntity<JornalResponse> agregarActividad(@PathVariable Long id,
                                                           @PathVariable Long actividadId) {
        return ResponseEntity.ok(JornalResponse.from(jornalUseCase.agregarActividad(id, actividadId)));
    }

    @DeleteMapping("/{id}/actividades/{actividadId}")
    public ResponseEntity<JornalResponse> removerActividad(@PathVariable Long id,
                                                           @PathVariable Long actividadId) {
        return ResponseEntity.ok(JornalResponse.from(jornalUseCase.removerActividad(id, actividadId)));
    }
}