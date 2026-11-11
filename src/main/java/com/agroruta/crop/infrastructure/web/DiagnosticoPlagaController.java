package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.DiagnosticoPlagaUseCase;
import com.agroruta.crop.domain.DiagnosticoPlaga;
import com.agroruta.crop.domain.DiagnosticosSiembra;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.security.CurrentUser;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

/** Diagnóstico de plagas y enfermedades con una foto, dentro de la bitácora de una siembra. */
@RestController
@RequestMapping("/api/plantings/{id}/diagnoses")
public class DiagnosticoPlagaController {

    private final DiagnosticoPlagaUseCase diagnosticos;

    public DiagnosticoPlagaController(DiagnosticoPlagaUseCase diagnosticos) {
        this.diagnosticos = diagnosticos;
    }

    @GetMapping
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<DiagnosticosSiembra> listar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(diagnosticos.deSiembra(id, CurrentUser.id(auth)));
    }

    /** Recibe la foto (campo "foto") y, si se quiere, una nota sobre lo que se ve (campo "nota") */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<DiagnosticoPlaga> diagnosticar(@PathVariable Long id,
                                                         @RequestParam("foto") MultipartFile foto,
                                                         @RequestParam(value = "nota", required = false) String nota,
                                                         Authentication auth) {
        byte[] archivo;
        try {
            archivo = foto.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "No se pudo leer la foto. Inténtalo de nuevo.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(diagnosticos.diagnosticar(id, CurrentUser.id(auth), archivo, nota));
    }

    @GetMapping("/{diagnosticoId}/photo")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<byte[]> foto(@PathVariable Long id, @PathVariable Long diagnosticoId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                // La foto de un diagnóstico no cambia; solo la guarda el navegador de quien la pidió
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(diagnosticos.foto(id, diagnosticoId));
    }

    @DeleteMapping("/{diagnosticoId}")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @PathVariable Long diagnosticoId) {
        diagnosticos.eliminar(id, diagnosticoId);
        return ResponseEntity.noContent().build();
    }
}
