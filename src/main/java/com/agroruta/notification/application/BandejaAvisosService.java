package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.in.BandejaAvisosUseCase;
import com.agroruta.notification.application.ports.out.BandejaAvisosRepository;
import com.agroruta.notification.domain.AvisoBandeja;
import com.agroruta.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BandejaAvisosService implements BandejaAvisosUseCase {

    private static final Logger log = LoggerFactory.getLogger(BandejaAvisosService.class);
    /** Lo que muestra la campana; lo más viejo sigue en la base hasta que la limpieza diaria lo borre */
    static final int RECIENTES = 30;

    private final BandejaAvisosRepository avisos;
    private final Clock reloj;

    public BandejaAvisosService(BandejaAvisosRepository avisos, Clock reloj) {
        this.avisos = avisos;
        this.reloj = reloj;
    }

    @Override
    public void avisar(AvisoBandeja.Nuevo aviso) {
        if (aviso == null || aviso.usuarioId() == null || aviso.tipo() == null || aviso.etiqueta() == null) return;
        try {
            avisos.guardar(aviso, Instant.now(reloj));
        } catch (RuntimeException e) {
            log.warn(
                    "No se pudo guardar el aviso {} del usuario {} en la bandeja",
                    aviso.etiqueta(),
                    aviso.usuarioId(),
                    e);
        }
    }

    @Override
    public List<AvisoBandeja> recientes(Long usuarioId) {
        return avisos.recientes(usuarioId, RECIENTES);
    }

    @Override
    public int noLeidos(Long usuarioId) {
        return avisos.noLeidos(usuarioId);
    }

    @Override
    public void marcarLeido(Long usuarioId, Long avisoId) {
        // Un aviso de otra persona responde igual que uno que no existe
        if (!avisos.marcarLeido(usuarioId, avisoId, Instant.now(reloj))) {
            throw new ResourceNotFoundException("Aviso", avisoId);
        }
    }

    @Override
    public void marcarTodosLeidos(Long usuarioId) {
        avisos.marcarTodosLeidos(usuarioId, Instant.now(reloj));
    }
}
