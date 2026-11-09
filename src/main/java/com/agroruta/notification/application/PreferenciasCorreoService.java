package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.in.PreferenciasCorreoUseCase;
import com.agroruta.notification.domain.PreferenciasCorreo;
import com.agroruta.notification.application.ports.out.PreferenciasCorreoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenciasCorreoService implements PreferenciasCorreoUseCase {

    private final PreferenciasCorreoRepository repository;

    public PreferenciasCorreoService(PreferenciasCorreoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PreferenciasCorreo obtener(Long usuarioId) {
        return repository.buscar(usuarioId).orElse(PreferenciasCorreo.porDefecto(usuarioId));
    }

    @Override
    @Transactional
    public PreferenciasCorreo guardar(Long usuarioId, boolean alertasCultivo, boolean avisosTrabajadores) {
        return repository.guardar(new PreferenciasCorreo(usuarioId, alertasCultivo, avisosTrabajadores));
    }
}
