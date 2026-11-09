package com.agroruta.worker.infrastructure.web;

import com.agroruta.worker.domain.ResumenJornales;
import com.agroruta.shared.util.Pagina;
import jakarta.validation.Valid;
import com.agroruta.shared.security.CurrentUser;
import com.agroruta.worker.application.ports.in.JornalUseCase;
import com.agroruta.worker.application.ports.out.CultivoPort;
import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.infrastructure.web.dto.JornalRequest;
import com.agroruta.worker.infrastructure.web.dto.JornalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/workdays")
public class JornalController {

    private final JornalUseCase jornalUseCase;

    public JornalController(JornalUseCase jornalUseCase) {
        this.jornalUseCase = jornalUseCase;
    }

    @PostMapping
    @PreAuthorize("@ownership.trabajadorPropio(#req.trabajadorId, authentication)")
    public ResponseEntity<JornalResponse> registrar(@Valid @RequestBody JornalRequest req) {
        JornalResponse response = JornalResponse.from(
                jornalUseCase.registrarJornal(
                        req.trabajadorId, req.cultivoId, req.nombreCultivo,
                        req.fecha, req.actividadIds, req.observaciones
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Cultivos activos de las fincas del usuario, para el selector del formulario de jornal
    @GetMapping("/available-plantings")
    @PreAuthorize("hasAnyRole('Agricultor','Administrador')")
    public ResponseEntity<List<CultivoPort.CultivoInfo>> cultivosDisponibles(Authentication authentication) {
        return ResponseEntity.ok(jornalUseCase.cultivosDisponibles(CurrentUser.id(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.jornalPropio(#id, authentication)")
    public ResponseEntity<JornalResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(JornalResponse.from(jornalUseCase.buscarPorId(id)));
    }

    @GetMapping("/worker/{trabajadorId}")
    @PreAuthorize("@ownership.trabajadorPropio(#trabajadorId, authentication)")
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

    // cultivoId es un número libre (no pertenece a un solo usuario): se devuelven
    // solo los jornales de los trabajadores del usuario autenticado.
    @GetMapping("/planting/{cultivoId}")
    public ResponseEntity<List<JornalResponse>> porCultivo(
            @PathVariable Long cultivoId,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fin,
            Authentication authentication) {
        List<Jornal> jornales = (inicio != null && fin != null)
                ? jornalUseCase.listarPorCultivoYPeriodo(cultivoId, inicio, fin)
                : jornalUseCase.listarPorCultivo(cultivoId);
        List<JornalResponse> respuesta = soloLosDelUsuario(jornales, authentication)
                .stream().map(JornalResponse::from).toList();
        return ResponseEntity.ok(respuesta);
    }

    @PatchMapping("/{id}/activities/{actividadId}")
    @PreAuthorize("@ownership.jornalPropio(#id, authentication)")
    public ResponseEntity<JornalResponse> agregarActividad(@PathVariable Long id,
                                                           @PathVariable Long actividadId) {
        return ResponseEntity.ok(JornalResponse.from(jornalUseCase.agregarActividad(id, actividadId)));
    }

    @DeleteMapping("/{id}/activities/{actividadId}")
    @PreAuthorize("@ownership.jornalPropio(#id, authentication)")
    public ResponseEntity<JornalResponse> removerActividad(@PathVariable Long id,
                                                           @PathVariable Long actividadId) {
        return ResponseEntity.ok(JornalResponse.from(jornalUseCase.removerActividad(id, actividadId)));
    }

    /** Administrador ve todo; el resto solo los jornales de sus trabajadores. */
    private List<Jornal> soloLosDelUsuario(List<Jornal> jornales, Authentication auth) {
        if (CurrentUser.esAdmin(auth)) return jornales;
        Long userId = CurrentUser.id(auth);
        if (userId == null) return List.of();
        return jornales.stream()
                .filter(j -> j.getTrabajador() != null
                        && userId.equals(j.getTrabajador().getAgricultorId()))
                .toList();
    }

    /** Jornales del trabajador por páginas, más recientes primero. inicio y fin son opcionales. */
    @GetMapping("/worker/{trabajadorId}/page")
    @PreAuthorize("@ownership.trabajadorPropio(#trabajadorId, authentication)")
    public ResponseEntity<Pagina<JornalResponse>> paginaPorTrabajador(
            @PathVariable Long trabajadorId,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fin,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(jornalUseCase.paginaPorTrabajador(trabajadorId, inicio, fin,
                Pagina.numero(page), Pagina.tamano(size)).map(JornalResponse::from));
    }

    /** Totales de los jornales del trabajador en el mismo rango (los muestra la lista paginada). */
    @GetMapping("/worker/{trabajadorId}/summary")
    @PreAuthorize("@ownership.trabajadorPropio(#trabajadorId, authentication)")
    public ResponseEntity<ResumenJornales> resumenPorTrabajador(
            @PathVariable Long trabajadorId,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fin) {
        return ResponseEntity.ok(jornalUseCase.resumenPorTrabajador(trabajadorId, inicio, fin));
    }
}
