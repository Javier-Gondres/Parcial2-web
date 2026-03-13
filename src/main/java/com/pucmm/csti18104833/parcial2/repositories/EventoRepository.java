package com.pucmm.csti18104833.parcial2.repositories;

import com.pucmm.csti18104833.parcial2.entities.EstadoEvento;
import com.pucmm.csti18104833.parcial2.entities.EventoEntity;
import com.pucmm.csti18104833.parcial2.entities.UsuarioEntity;
import com.pucmm.csti18104833.parcial2.util.JpaUtil;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class EventoRepository {
    public Optional<EventoEntity> findById(Long id) {
        return JpaUtil.query(entityManager -> Optional.ofNullable(entityManager.find(EventoEntity.class, id)));
    }

    public EventoEntity findByIdOrNull(EntityManager entityManager, Long id) {
        return entityManager.find(EventoEntity.class, id);
    }

    public List<EventoEntity> findVisibleForPublic() {
        return JpaUtil.query(entityManager -> entityManager.createQuery(
                "select e from EventoEntity e where e.estado = :estado order by e.fechaHora asc",
                EventoEntity.class
            )
            .setParameter("estado", EstadoEvento.PUBLICADO)
            .getResultList());
    }

    public List<EventoEntity> findAll() {
        return JpaUtil.query(entityManager -> entityManager.createQuery(
                "select e from EventoEntity e order by e.fechaHora asc",
                EventoEntity.class
            )
            .getResultList());
    }

    public List<EventoEntity> findByCreator(UsuarioEntity creador) {
        return JpaUtil.query(entityManager -> entityManager.createQuery(
                "select e from EventoEntity e where e.creadoPor.id = :creadorId order by e.fechaHora asc",
                EventoEntity.class
            )
            .setParameter("creadorId", creador.getId())
            .getResultList());
    }

    public EventoEntity save(EventoEntity evento) {
        return JpaUtil.transaction(entityManager -> save(entityManager, evento));
    }

    public EventoEntity save(EntityManager entityManager, EventoEntity evento) {
        if (evento.getId() == null) {
            entityManager.persist(evento);
            return evento;
        }
        return entityManager.merge(evento);
    }

    public void delete(EntityManager entityManager, EventoEntity evento) {
        EventoEntity managed = entityManager.contains(evento) ? evento : entityManager.merge(evento);
        entityManager.remove(managed);
    }
}
