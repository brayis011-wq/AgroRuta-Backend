package com.agroruta.cultivo.infrastructure.persistence;

import com.agroruta.cultivo.domain.Lote;
import com.agroruta.cultivo.domain.LoteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class LoteRepositoryImpl implements LoteRepository {

    private final JpaLoteRepository jpaRepository;
    private final JpaSiembraRepository jpaSiembraRepository;

    public LoteRepositoryImpl(JpaLoteRepository jpaRepository,
                              JpaSiembraRepository jpaSiembraRepository) {
        this.jpaRepository = jpaRepository;
        this.jpaSiembraRepository = jpaSiembraRepository;
    }

    @Override
    public Lote save(Lote lote) {
        LoteEntity entity = toEntity(lote);
        LoteEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Lote> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Lote> findByFincaId(Long fincaId) {
        return jpaRepository.findByFincaId(fincaId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsSiembraActivaEnLote(Long loteId) {
        return jpaSiembraRepository.findByLoteId(loteId).isPresent();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private LoteEntity toEntity(Lote lote) {
        LoteEntity entity = new LoteEntity();
        entity.setId(lote.getId());
        entity.setNombre(lote.getNombre());
        entity.setArea(lote.getArea());
        entity.setEstado(lote.getEstado());
        entity.setFincaId(lote.getFincaId());
        return entity;
    }

    private Lote toDomain(LoteEntity entity) {
        Lote lote = new Lote();
        lote.setId(entity.getId());
        lote.setNombre(entity.getNombre());
        lote.setArea(entity.getArea());
        lote.setEstado(entity.getEstado());
        lote.setFincaId(entity.getFincaId());
        return lote;
    }
}