package com.agroruta.user.infrastructure.persistence;

import com.agroruta.user.application.ports.out.SesionRepository;
import com.agroruta.user.domain.Sesion;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class SesionRepositoryImpl implements SesionRepository {

    private final JpaSesionRepository jpa;

    public SesionRepositoryImpl(JpaSesionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Sesion guardar(Sesion s) {
        SesionEntity e = new SesionEntity();
        e.setId(s.getId());
        e.setUsuarioId(s.getUsuarioId());
        e.setTokenHash(s.getTokenHash());
        e.setCreadoEn(s.getCreadoEn());
        e.setExpiraEn(s.getExpiraEn());
        e.setRotadoEn(s.getRotadoEn());
        e.setRevocadoEn(s.getRevocadoEn());
        return aDominio(jpa.save(e));
    }

    @Override
    public Optional<Sesion> buscarPorTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(SesionRepositoryImpl::aDominio);
    }

    @Override
    public void revocarTodas(Long usuarioId, LocalDateTime ahora) {
        jpa.revocarTodas(usuarioId, ahora);
    }

    @Override
    public void borrarVencidas(Long usuarioId, LocalDateTime ahora) {
        jpa.borrarVencidas(usuarioId, ahora);
    }

    private static Sesion aDominio(SesionEntity e) {
        return new Sesion(e.getId(), e.getUsuarioId(), e.getTokenHash(), e.getCreadoEn(), e.getExpiraEn(),
                e.getRotadoEn(), e.getRevocadoEn());
    }
}
