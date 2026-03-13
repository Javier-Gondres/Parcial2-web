package com.pucmm.csti18104833.parcial2.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.util.function.Function;

public final class JpaUtil {
    private static EntityManagerFactory entityManagerFactory;

    private JpaUtil() {
    }

    public static synchronized void initialize() {
        if (entityManagerFactory == null) {
            entityManagerFactory = Persistence.createEntityManagerFactory("parcial2PU");
        }
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        initialize();
        return entityManagerFactory;
    }

    public static EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public static <T> T query(Function<EntityManager, T> work) {
        try (EntityManager entityManager = createEntityManager()) {
            return work.apply(entityManager);
        }
    }

    public static <T> T transaction(Function<EntityManager, T> work) {
        EntityManager entityManager = createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            T result = work.apply(entityManager);
            transaction.commit();
            return result;
        } catch (RuntimeException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            entityManager.close();
        }
    }

    public static synchronized void shutdown() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
        entityManagerFactory = null;
    }
}
