package com.pucmm.csti18104833.parcial2.bootstrap;

import com.pucmm.csti18104833.parcial2.entities.RolUsuario;
import com.pucmm.csti18104833.parcial2.entities.UsuarioEntity;
import com.pucmm.csti18104833.parcial2.util.H2ServerManager;
import com.pucmm.csti18104833.parcial2.util.JpaUtil;
import com.pucmm.csti18104833.parcial2.util.PasswordUtil;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;

public final class DatabaseBootstrap {
    private DatabaseBootstrap() {
    }

    public static void initialize() {
        H2ServerManager.start();
        JpaUtil.initialize();
        seedAdmin();
    }

    public static void shutdown() {
        JpaUtil.shutdown();
        H2ServerManager.stop();
    }

    private static void seedAdmin() {
        JpaUtil.transaction(entityManager -> {
            TypedQuery<UsuarioEntity> query = entityManager.createQuery(
                "select u from UsuarioEntity u where u.usuario = :usuario",
                UsuarioEntity.class
            );
            query.setParameter("usuario", "admin");

            if (query.getResultStream().findFirst().isPresent()) {
                return null;
            }

            UsuarioEntity admin = new UsuarioEntity();
            admin.setUsuario("admin");
            admin.setNombre("Administrador General");
            admin.setPasswordHash(PasswordUtil.hash("123456"));
            admin.setRol(RolUsuario.ADMINISTRADOR);
            admin.setBloqueado(false);
            admin.setFechaCreacion(LocalDateTime.now());
            entityManager.persist(admin);
            return null;
        });
    }
}
