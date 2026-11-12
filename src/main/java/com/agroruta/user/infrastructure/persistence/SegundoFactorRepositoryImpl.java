package com.agroruta.user.infrastructure.persistence;

import com.agroruta.user.application.ports.out.SegundoFactorRepository;
import com.agroruta.user.domain.SegundoFactor;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class SegundoFactorRepositoryImpl implements SegundoFactorRepository {

    private final JpaSegundoFactorRepository jpa;

    public SegundoFactorRepositoryImpl(JpaSegundoFactorRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<SegundoFactor> buscar(Long usuarioId) {
        return usuarioId == null ? Optional.empty() : jpa.findById(usuarioId).map(SegundoFactorRepositoryImpl::aDominio);
    }

    @Override
    public SegundoFactor guardar(SegundoFactor s) {
        SegundoFactorEntity e = new SegundoFactorEntity();
        e.setUsuarioId(s.getUsuarioId());
        e.setSecreto(s.getSecretoCifrado());
        e.setActivo(s.isActivo());
        e.setRespaldo(s.getRespaldo().isEmpty() ? null : String.join(",", s.getRespaldo()));
        e.setUltimoPaso(s.getUltimoPaso());
        e.setCreadoEn(s.getCreadoEn());
        e.setActivadoEn(s.getActivadoEn());
        return aDominio(jpa.save(e));
    }

    @Override
    public void borrar(Long usuarioId) {
        if (jpa.existsById(usuarioId)) jpa.deleteById(usuarioId);
    }

    private static SegundoFactor aDominio(SegundoFactorEntity e) {
        List<String> respaldo = e.getRespaldo() == null || e.getRespaldo().isBlank()
                ? List.of() : Arrays.asList(e.getRespaldo().split(","));
        return new SegundoFactor(e.getUsuarioId(), e.getSecreto(), e.isActivo(), respaldo,
                e.getUltimoPaso(), e.getCreadoEn(), e.getActivadoEn());
    }
}
