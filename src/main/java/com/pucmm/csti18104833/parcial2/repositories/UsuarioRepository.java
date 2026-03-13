package com.pucmm.csti18104833.parcial2.repositories;

import com.pucmm.csti18104833.parcial2.entities.UsuarioEntity;
import com.pucmm.csti18104833.parcial2.util.JpaUtil;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class UsuarioRepository {
    public Optional<UsuarioEntity> findById(Long id) {
        return JpaUtil.query(entityManager -> Optional.ofNullable(entityManager.find(UsuarioEntity.class, id)));
    }

    public Optional<UsuarioEntity> findByUsuario(String usuario) {
        return JpaUtil.query(entityManager -> entityManager.createQuery(
                "select u from UsuarioEntity u where lower(u.usuario) = :usuario",
                UsuarioEntity.class
            )
            .setParameter("usuario", usuario.toLowerCase())
            .getResultStream()
            .findFirst());
    }

    public List<UsuarioEntity> findAll() {
        return JpaUtil.query(entityManager -> entityManager.createQuery(
                "select u from UsuarioEntity u order by u.id asc",
                UsuarioEntity.class
            )
            .getResultList());
    }

    public UsuarioEntity save(UsuarioEntity usuario) {
        return JpaUtil.transaction(entityManager -> save(entityManager, usuario));
    }

    public UsuarioEntity save(EntityManager entityManager, UsuarioEntity usuario) {
        if (usuario.getId() == null) {
            entityManager.persist(usuario);
            return usuario;
        }
        return entityManager.merge(usuario);
    }
}
