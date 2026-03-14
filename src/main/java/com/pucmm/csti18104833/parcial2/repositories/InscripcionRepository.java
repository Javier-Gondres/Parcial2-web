package com.pucmm.csti18104833.parcial2.repositories;

import com.pucmm.csti18104833.parcial2.entities.InscripcionEntity;
import com.pucmm.csti18104833.parcial2.util.JpaUtil;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class InscripcionRepository {
    public Optional<InscripcionEntity> findByToken(String token) {
        return JpaUtil.query(entityManager -> entityManager.createQuery(
                "select i from InscripcionEntity i " +
                    "join fetch i.usuario " +
                    "join fetch i.evento " +
                    "where i.tokenValidacion = :token",
                InscripcionEntity.class
            )
            .setParameter("token", token)
            .getResultStream()
            .findFirst());
    }

    public Optional<InscripcionEntity> findByUserAndEvent(Long userId, Long eventId) {
        return JpaUtil.query(entityManager -> entityManager.createQuery(
                "select i from InscripcionEntity i " +
                    "join fetch i.usuario " +
                    "join fetch i.evento " +
                    "where i.usuario.id = :userId and i.evento.id = :eventId",
                InscripcionEntity.class
            )
            .setParameter("userId", userId)
            .setParameter("eventId", eventId)
            .getResultStream()
            .findFirst());
    }

    public Optional<InscripcionEntity> findByUserAndEvent(EntityManager entityManager, Long userId, Long eventId) {
        return entityManager.createQuery(
                "select i from InscripcionEntity i " +
                    "where i.usuario.id = :userId and i.evento.id = :eventId",
                InscripcionEntity.class
            )
            .setParameter("userId", userId)
            .setParameter("eventId", eventId)
            .getResultStream()
            .findFirst();
    }

    public long countActivasByEvento(Long eventId) {
        return JpaUtil.query(entityManager -> entityManager.createQuery(
                "select count(i) from InscripcionEntity i " +
                    "where i.evento.id = :eventId and i.cancelada = false",
                Long.class
            )
            .setParameter("eventId", eventId)
            .getSingleResult());
    }

    public long countActivasByEvento(EntityManager entityManager, Long eventId) {
        return entityManager.createQuery(
                "select count(i) from InscripcionEntity i " +
                    "where i.evento.id = :eventId and i.cancelada = false",
                Long.class
            )
            .setParameter("eventId", eventId)
            .getSingleResult();
    }

    public List<InscripcionEntity> findByEvento(Long eventId) {
        return JpaUtil.query(entityManager -> entityManager.createQuery(
                "select i from InscripcionEntity i " +
                    "join fetch i.usuario " +
                    "join fetch i.evento " +
                    "where i.evento.id = :eventId and i.cancelada = false " +
                    "order by i.fechaInscripcion asc",
                InscripcionEntity.class
            )
            .setParameter("eventId", eventId)
            .getResultList());
    }

    public void deleteByEvento(EntityManager entityManager, Long eventId) {
        entityManager.createQuery("delete from InscripcionEntity i where i.evento.id = :eventId")
            .setParameter("eventId", eventId)
            .executeUpdate();
    }

    public InscripcionEntity save(InscripcionEntity inscripcion) {
        return JpaUtil.transaction(entityManager -> save(entityManager, inscripcion));
    }

    public InscripcionEntity save(EntityManager entityManager, InscripcionEntity inscripcion) {
        if (inscripcion.getId() == null) {
            entityManager.persist(inscripcion);
            return inscripcion;
        }
        return entityManager.merge(inscripcion);
    }
}
