package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.EtapaSiembra;
import com.agroruta.crop.application.ports.out.EtapaSiembraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EtapaSiembraRepositoryImpl implements EtapaSiembraRepository {

    private final JpaEtapaSiembraRepository jpa;

    public EtapaSiembraRepositoryImpl(JpaEtapaSiembraRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public EtapaSiembra save(EtapaSiembra etapa) {
        EtapaSiembraEntity e = new EtapaSiembraEntity();
        e.setId(etapa.getId());
        e.setSiembraId(etapa.getSiembraId());
        e.setEtapa(etapa.getEtapa());
        e.setFecha(etapa.getFecha());
        return toDomain(jpa.save(e));
    }

    @Override
    public List<EtapaSiembra> findBySiembraId(Long siembraId) {
        return jpa.findBySiembraIdOrderByFechaAscIdAsc(siembraId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<EtapaSiembra> findUltima(Long siembraId) {
        return jpa.findFirstBySiembraIdOrderByIdDesc(siembraId).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public void deleteBySiembraId(Long siembraId) {
        jpa.deleteBySiembraId(siembraId);
    }

    private EtapaSiembra toDomain(EtapaSiembraEntity e) {
        return new EtapaSiembra(e.getId(), e.getSiembraId(), e.getEtapa(), e.getFecha());
    }
}
