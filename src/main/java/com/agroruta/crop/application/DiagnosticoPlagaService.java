package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.DiagnosticoPlagaUseCase;
import com.agroruta.crop.application.ports.out.DiagnosticoIaPort;
import com.agroruta.crop.application.ports.out.DiagnosticoPlagaRepository;
import com.agroruta.crop.application.ports.out.SiembraRepository;
import com.agroruta.crop.domain.AnalisisFoto;
import com.agroruta.crop.domain.ConfianzaDiagnostico;
import com.agroruta.crop.domain.ContextoCultivo;
import com.agroruta.crop.domain.DiagnosticoPlaga;
import com.agroruta.crop.domain.DiagnosticosSiembra;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.crop.domain.TipoProblema;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ExternalApiException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Diagnóstico de plagas y enfermedades con una foto. La foto se reduce y se le quitan los
 * metadatos antes de analizarla; en la bitácora queda una copia pequeña junto al resultado.
 */
@Service
public class DiagnosticoPlagaService implements DiagnosticoPlagaUseCase {

    static final int LIMITE_LISTA = 30;
    private static final int MAX_NOTA = 300;

    private final DiagnosticoPlagaRepository repositorio;
    private final DiagnosticoIaPort ia;
    private final SiembraRepository siembras;
    private final int maxPorDia;
    private final int maxTotalDia;
    private final Clock reloj;

    @Autowired
    public DiagnosticoPlagaService(DiagnosticoPlagaRepository repositorio, DiagnosticoIaPort ia, SiembraRepository siembras,
                                   @Value("${app.diagnostico.max-por-dia:15}") int maxPorDia,
                                   @Value("${app.diagnostico.max-total-dia:300}") int maxTotalDia) {
        this(repositorio, ia, siembras, maxPorDia, maxTotalDia, Clock.system(ZoneId.of("America/Bogota")));
    }

    DiagnosticoPlagaService(DiagnosticoPlagaRepository repositorio, DiagnosticoIaPort ia, SiembraRepository siembras,
                            int maxPorDia, int maxTotalDia, Clock reloj) {
        this.repositorio = repositorio;
        this.ia = ia;
        this.siembras = siembras;
        this.maxPorDia = maxPorDia;
        this.maxTotalDia = maxTotalDia;
        this.reloj = reloj;
    }

    @Override
    public DiagnosticosSiembra deSiembra(Long siembraId, Long usuarioId) {
        int restantes = usuarioId == null ? 0 : Math.max(0, maxPorDia - repositorio.usoDe(usuarioId, LocalDate.now(reloj)));
        return new DiagnosticosSiembra(ia.disponible(), restantes, repositorio.deSiembra(siembraId, LIMITE_LISTA));
    }

    @Override
    public DiagnosticoPlaga diagnosticar(Long siembraId, Long usuarioId, byte[] archivo, String nota) {
        if (!ia.disponible()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "El diagnóstico con foto no está activado en este servidor.");
        }
        Siembra siembra = siembras.findById(siembraId)
                .filter(s -> !s.estaEliminada())
                .orElseThrow(() -> new ResourceNotFoundException("Siembra", siembraId));
        FotoDiagnostico.Preparada foto = FotoDiagnostico.preparar(archivo);
        String notaLimpia = limpiar(nota, MAX_NOTA);

        LocalDate hoy = LocalDate.now(reloj);
        if (repositorio.usoDe(usuarioId, hoy) >= maxPorDia) {
            throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS,
                    "Ya usaste los " + maxPorDia + " diagnósticos de hoy. Mañana puedes analizar más fotos.");
        }
        if (repositorio.usoTotal(hoy) >= maxTotalDia) {
            throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS,
                    "El diagnóstico con foto llegó a su tope por hoy. Inténtalo mañana.");
        }

        // Se cuenta antes de llamar: el análisis se cobra aunque la foto no sirva
        repositorio.anotarUso(usuarioId, hoy);
        AnalisisFoto analisis;
        try {
            analisis = ia.analizar(foto.paraAnalizar(), contexto(siembra, notaLimpia, hoy))
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OPERATION,
                            "No se pudo analizar esta foto. Prueba con otra donde se vea de cerca la parte afectada."));
        } catch (ExternalApiException e) {
            repositorio.devolverUso(usuarioId, hoy);
            throw e;
        }
        if (!analisis.esPlanta()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, mensajeSinPlanta(analisis.problema()));
        }
        return repositorio.guardar(siembraId, usuarioId, LocalDateTime.now(reloj), foto.paraGuardar(), notaLimpia, ordenar(analisis));
    }

    @Override
    public byte[] foto(Long siembraId, Long diagnosticoId) {
        return repositorio.foto(siembraId, diagnosticoId)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnóstico", diagnosticoId));
    }

    @Override
    public void eliminar(Long siembraId, Long diagnosticoId) {
        if (!repositorio.eliminar(siembraId, diagnosticoId)) {
            throw new ResourceNotFoundException("Diagnóstico", diagnosticoId);
        }
    }

    private static ContextoCultivo contexto(Siembra siembra, String nota, LocalDate hoy) {
        Long dias = siembra.getFechaSiembra() == null ? null : Math.max(0, ChronoUnit.DAYS.between(siembra.getFechaSiembra(), hoy));
        String etapa = siembra.getEstadoCultivo() == null ? null
                : siembra.getEstadoCultivo().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return new ContextoCultivo(siembra.getTipoCultivo(), siembra.getVariedad(), etapa, dias, nota);
    }

    /** Deja la respuesta dentro de lo que cabe en la bitácora y sin campos vacíos */
    private static AnalisisFoto ordenar(AnalisisFoto a) {
        String problema = limpiar(a.problema(), 160);
        return new AnalisisFoto(true,
                problema == null ? "No se pudo determinar" : problema,
                a.tipo() == null ? TipoProblema.INDETERMINADO : a.tipo(),
                a.confianza() == null ? ConfianzaDiagnostico.BAJA : a.confianza(),
                limpiar(a.sintomas(), 1200),
                lista(a.recomendaciones(), 6),
                lista(a.otrasPosibilidades(), 4),
                limpiar(a.advertencia(), 600),
                limpiar(a.modelo(), 60));
    }

    private static String mensajeSinPlanta(String explicacion) {
        String detalle = limpiar(explicacion, 200);
        return "En la foto no se distingue un cultivo que se pueda evaluar."
                + (detalle == null ? " Toma otra de cerca, con buena luz, de la hoja, el tallo o el fruto afectado." : " " + detalle);
    }

    private static List<String> lista(List<String> textos, int maximo) {
        if (textos == null) return List.of();
        return textos.stream().map(t -> limpiar(t, 400)).filter(Objects::nonNull).limit(maximo).toList();
    }

    /** Una sola línea, sin espacios de sobra y recortada; null si queda vacía */
    private static String limpiar(String texto, int maximo) {
        if (texto == null) return null;
        String limpio = texto.replaceAll("\\s+", " ").trim();
        if (limpio.isEmpty()) return null;
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo - 1).trim() + "…";
    }
}
