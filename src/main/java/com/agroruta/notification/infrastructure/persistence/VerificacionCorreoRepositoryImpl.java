package com.agroruta.notification.infrastructure.persistence;

import com.agroruta.notification.domain.VerificacionCorreo;
import com.agroruta.notification.application.ports.out.VerificacionCorreoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class VerificacionCorreoRepositoryImpl implements VerificacionCorreoRepository {

    private final JpaVerificacionCorreoRepository jpa;

    public VerificacionCorreoRepositoryImpl(JpaVerificacionCorreoRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public VerificacionCorreo guardar(VerificacionCorreo v) {
        VerificacionCorreoEntity e = new VerificacionCorreoEntity();
        e.setId(v.getId());
        e.setUsuarioId(v.getUsuarioId());
        e.setTokenHash(v.getTokenHash());
        e.setCreadoEn(v.getCreadoEn());
        e.setExpiraEn(v.getExpiraEn());
        e.setUsadoEn(v.getUsadoEn());
        return aDominio(jpa.save(e));
    }

    @Override
    public Optional<VerificacionCorreo> buscarPorTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(VerificacionCorreoRepositoryImpl::aDominio);
    }

    @Override
    public long contarDesde(Long usuarioId, LocalDateTime desde) {
        return jpa.countByUsuarioIdAndCreadoEnAfter(usuarioId, desde);
    }

    @Override
    public void anularPendientes(Long usuarioId, LocalDateTime ahora) {
        jpa.anularPendientes(usuarioId, ahora);
    }

    private static VerificacionCorreo aDominio(VerificacionCorreoEntity e) {
        return new VerificacionCorreo(e.getId(), e.getUsuarioId(), e.getTokenHash(),
                e.getCreadoEn(), e.getExpiraEn(), e.getUsadoEn());
    }
}
