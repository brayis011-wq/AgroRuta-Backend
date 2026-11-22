package com.agroruta.worker.infrastructure.web;

import com.agroruta.worker.domain.ResumenNominas;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.shared.util.Pagina;
import jakarta.validation.Valid;
import com.agroruta.shared.security.CurrentUser;
import com.agroruta.worker.application.ports.in.NominaUseCase;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.infrastructure.web.dto.NominaRequest;
import com.agroruta.worker.infrastructure.web.dto.NominaResponse;
import com.agroruta.worker.infrastructure.web.dto.NominasEquipoRequest;
import com.agroruta.worker.infrastructure.web.dto.IdsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payrolls")
public class NominaController {

    private final NominaUseCase nominaUseCase;

    public NominaController(NominaUseCase nominaUseCase) {
        this.nominaUseCase = nominaUseCase;
    }

    @PostMapping("/generate")
    @PreAuthorize("@ownership.trabajadorPropio(#req.trabajadorId, authentication)")
    public ResponseEntity<NominaResponse> generar(@Valid @RequestBody NominaRequest req) {
        NominaResponse response = NominaResponse.from(
                nominaUseCase.generarNomina(req.trabajadorId, req.periodoInicio, req.periodoFin)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** La quincena de todo el equipo en un paso (una nómina por trabajador con jornales en el periodo). */
    @PostMapping("/generate-all")
    @PreAuthorize("hasRole('Agricultor')")
    public ResponseEntity<List<NominaResponse>> generarParaTodos(@Valid @RequestBody NominasEquipoRequest req,
                                                                 Authentication authentication) {
        List<NominaResponse> creadas = nominaUseCase
                .generarParaTodos(CurrentUser.id(authentication), req.periodoInicio, req.periodoFin)
                .stream().map(NominaResponse::from).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(creadas);
    }

    /** Aprueba varias nóminas pendientes a la vez. */
    @PatchMapping("/approve-batch")
    @PreAuthorize("hasAnyRole('Agricultor','Administrador')")
    public ResponseEntity<List<NominaResponse>> aprobarVarias(@Valid @RequestBody IdsRequest req,
                                                              Authentication authentication) {
        return ResponseEntity.ok(nominaUseCase.aprobarVarias(dueno(authentication), req.ids)
                .stream().map(NominaResponse::from).toList());
    }

    /** Corrige una nómina pendiente: le quita un jornal y recalcula el total. */
    @DeleteMapping("/{id}/workdays/{jornalId}")
    @PreAuthorize("@ownership.nominaPropia(#id, authentication)")
    public ResponseEntity<NominaResponse> quitarJornal(@PathVariable Long id, @PathVariable Long jornalId) {
        return ResponseEntity.ok(NominaResponse.from(nominaUseCase.quitarJornal(id, jornalId)));
    }

    /** Suma a una nómina pendiente los jornales de su periodo anotados después de generarla. */
    @PostMapping("/{id}/refresh")
    @PreAuthorize("@ownership.nominaPropia(#id, authentication)")
    public ResponseEntity<NominaResponse> traerJornalesNuevos(@PathVariable Long id) {
        return ResponseEntity.ok(NominaResponse.from(nominaUseCase.traerJornalesNuevos(id)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.nominaPropia(#id, authentication)")
    public ResponseEntity<NominaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(NominaResponse.from(nominaUseCase.buscarPorId(id)));
    }

    @GetMapping("/worker/{trabajadorId}")
    @PreAuthorize("@ownership.trabajadorPropio(#trabajadorId, authentication)")
    public ResponseEntity<List<NominaResponse>> porTrabajador(@PathVariable Long trabajadorId) {
        List<NominaResponse> nominas = nominaUseCase.listarPorTrabajador(trabajadorId)
                .stream().map(NominaResponse::from).toList();
        return ResponseEntity.ok(nominas);
    }

    @GetMapping
    public ResponseEntity<List<NominaResponse>> listarTodas(Authentication authentication) {
        List<Nomina> lista = CurrentUser.esAdmin(authentication)
                ? nominaUseCase.listarTodas()
                : nominaUseCase.listarTodasDe(CurrentUser.id(authentication));
        return ResponseEntity.ok(lista.stream().map(NominaResponse::from).toList());
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("@ownership.nominaPropia(#id, authentication)")
    public ResponseEntity<NominaResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(NominaResponse.from(nominaUseCase.aprobarNomina(id)));
    }

    @PatchMapping("/{id}/void")
    @PreAuthorize("@ownership.nominaPropia(#id, authentication)")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        nominaUseCase.anularNomina(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ NUEVO — vuelve la nómina a estado BORRADOR
    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("@ownership.nominaPropia(#id, authentication)")
    public ResponseEntity<NominaResponse> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(NominaResponse.from(nominaUseCase.reactivarNomina(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.nominaPropia(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        nominaUseCase.eliminarNomina(id);
        return ResponseEntity.noContent().build();
    }

    /** Lista de nóminas por páginas, más recientes primero. Filtros opcionales: trabajador y estado. */
    @GetMapping("/page")
    public ResponseEntity<Pagina<NominaResponse>> pagina(@RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size,
                                                         @RequestParam(required = false) Long trabajadorId,
                                                         @RequestParam(required = false) EstadoNomina estado,
                                                         Authentication authentication) {
        return ResponseEntity.ok(nominaUseCase.pagina(dueno(authentication), trabajadorId, estado,
                Pagina.numero(page), Pagina.tamano(size)).map(NominaResponse::from));
    }

    /** Cantidad y total por estado (tarjetas de la lista), con el mismo filtro de trabajador. */
    @GetMapping("/summary")
    public ResponseEntity<List<ResumenNominas>> resumen(@RequestParam(required = false) Long trabajadorId,
                                                        Authentication authentication) {
        return ResponseEntity.ok(nominaUseCase.resumenPorEstado(dueno(authentication), trabajadorId));
    }

    /** El agricultor solo ve lo suyo; el Administrador ve todo (null). */
    private static Long dueno(Authentication authentication) {
        return CurrentUser.esAdmin(authentication) ? null : CurrentUser.id(authentication);
    }
}
