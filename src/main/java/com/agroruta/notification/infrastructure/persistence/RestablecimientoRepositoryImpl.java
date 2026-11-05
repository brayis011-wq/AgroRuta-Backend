package com.agroruta.notification.infrastructure.persistence;

import com.agroruta.notification.domain.RestablecimientoContrasena;
import com.agroruta.notification.domain.RestablecimientoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class RestablecimientoRepositoryImpl implements RestablecimientoRepository {

    private final JpaRestablecimientoRepository jpa;

    public RestablecimientoRepositoryImpl(JpaRestablecimientoRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RestablecimientoContrasena guardar(RestablecimientoContrasena r) {
        RestablecimientoEntity e = new RestablecimientoEntity();
        e.setId(r.getId());
        e.setUsuarioId(r.getUsuarioId());
        e.setTokenHash(r.getTokenHash());
        e.setCreadoEn(r.getCreadoEn());
        e.setExpiraEn(r.getExpiraEn());
        e.setUsadoEn(r.getUsadoEn());
        return aDominio(jpa.save(e));
    }

    @Override
    public Optional<RestablecimientoContrasena> buscarPorTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RestablecimientoRepositoryImpl::aDominio);
    }

    @Override
    public long contarDesde(Long usuarioId, LocalDateTime desde) {
        return jpa.countByUsuarioIdAndCreadoEnAfter(usuarioId, desde);
    }

    @Override
    public void anularPendientes(Long usuarioId, LocalDateTime ahora) {
        jpa.anularPendientes(usuarioId, ahora);
    }

    private static RestablecimientoContrasena aDominio(RestablecimientoEntity e) {
        return new RestablecimientoContrasena(e.getId(), e.getUsuarioId(), e.getTokenHash(),
                e.getCreadoEn(), e.getExpiraEn(), e.getUsadoEn());
    }
}
