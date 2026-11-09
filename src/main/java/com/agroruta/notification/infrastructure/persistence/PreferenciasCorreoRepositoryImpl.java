package com.agroruta.notification.infrastructure.persistence;

import com.agroruta.notification.domain.PreferenciasCorreo;
import com.agroruta.notification.application.ports.out.PreferenciasCorreoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PreferenciasCorreoRepositoryImpl implements PreferenciasCorreoRepository {

    private final JpaPreferenciasCorreoRepository jpa;

    public PreferenciasCorreoRepositoryImpl(JpaPreferenciasCorreoRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PreferenciasCorreo> buscar(Long usuarioId) {
        return jpa.findById(usuarioId)
                .map(e -> new PreferenciasCorreo(e.getUsuarioId(), e.isAlertasCultivo(), e.isAvisosTrabajadores()));
    }

    @Override
    public PreferenciasCorreo guardar(PreferenciasCorreo p) {
        PreferenciasCorreoEntity e = jpa.findById(p.usuarioId()).orElseGet(PreferenciasCorreoEntity::new);
        e.setUsuarioId(p.usuarioId());
        e.setAlertasCultivo(p.alertasCultivo());
        e.setAvisosTrabajadores(p.avisosTrabajadores());
        jpa.save(e);
        return p;
    }
}
