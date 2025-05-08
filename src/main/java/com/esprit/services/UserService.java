// Path: src/main/java/services/UserService.java
package com.esprit.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.esprit.models.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class UserService implements Service<User>, AutoCloseable {

    private static EntityManagerFactory emf;
    private final EntityManager em;

    // Remove the static initializer block and replace with lazy initialization
    private static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null || !emf.isOpen()) {
            try {
                emf = Persistence.createEntityManagerFactory("uniclubsPU");
                System.out.println("EntityManagerFactory created");
            } catch (PersistenceException e) {
                System.err.println("Failed to initialize persistence factory: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
        return emf;
    }

    public UserService() {
        try {
            em = getEntityManagerFactory().createEntityManager();
            System.out.println("EntityManager created successfully");
        } catch (PersistenceException e) {
            System.err.println("Failed to initialize persistence: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Improved singleton pattern with proper connection handling
    private static UserService instance;
    private static final Object LOCK = new Object();

    public static UserService getInstance() {
        UserService localInstance = instance;
        if (localInstance == null || !localInstance.em.isOpen()) {
            synchronized (LOCK) {
                localInstance = instance;
                if (localInstance == null || !localInstance.em.isOpen()) {
                    if (localInstance != null) {
                        localInstance.close(); // Close existing instance if it exists but EM is closed
                    }
                    instance = new UserService();
                    localInstance = instance;
                }
            }
        }
        return localInstance;
    }

    /// FASAKHNIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII ///////

    // public UserService() {
    // conn = DataSource.getInstance().getCnx();
    // }

    /// FASAKHNIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII ///////
    ///
    ///
    ///
    ///
    ///
    ///
    ///

    @Override
    public void ajouter(User user) {
        try {
            // Validate entity first
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<User>> violations = validator.validate(user);

            if (!violations.isEmpty()) {
                for (ConstraintViolation<User> v : violations) {
                    System.err.println(
                            "Validation Error: " + v.getPropertyPath() + " " + v.getMessage());
                }
                throw new ConstraintViolationException(violations);
            }

            // Proceed with persistence
            executeInTransaction(() -> {
                em.persist(user);
                em.flush(); // Force immediate insert to verify operation
                System.out.println("Persisted: " + user.getId());
            });
        } catch (Exception e) {
            System.err.println("Persistence Error: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to ensure transaction rollback
        }
    }

    @Override
    public void modifier(User user) {
        executeInTransaction(() -> {
            // Check if the entity is already managed by the current session
            boolean isManaged = em.contains(user);
            if (isManaged) {
                // If the entity is already managed, just let Hibernate handle it
                // No need to call merge explicitly
                System.out.println("Entity is already managed");
                // Ensure changes are flushed
                em.flush();
            } else {
                // Only merge if the entity is detached
                User merged = em.merge(user);
                System.out.println("Entity merged successfully: " + merged.getId());
                // Ensure changes are written immediately
                em.flush();
            }
        });
    }

    @Override
    public void supprimer(User user) {
        executeInTransaction(() -> {
            User managedUser = em.merge(user);
            em.remove(managedUser);
        });
    }

    @Override
    public List<User> recuperer() {
        return em.createQuery("SELECT u FROM User u", User.class).getResultList();
    }

    public User getById(int id) {
        return em.find(User.class, id);
    }

    public List<User> rechercherParNom(String keyword) {
        TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.lastName LIKE :keyword OR u.firstName LIKE :keyword",
                User.class);
        query.setParameter("keyword", "%" + keyword + "%");
        return query.getResultList();
    }

    public User findByEmail(String email) {
        try {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email",
                    User.class);
            query.setParameter("email", email);
            List<User> result = query.getResultList();
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            System.err.println("Error finding user by email: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public EntityManager getEntityManager() {
        return em;
    }

    private void executeInTransaction(Runnable operation) {
        try {
            em.getTransaction().begin();
            operation.run();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.err.println("Transaction failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Transaction failed", e);
        }
    }

    @Override
    public void close() {
        if (em != null && em.isOpen()) {
            em.close();
            System.out.println("EntityManager closed");
        }
    }

    // Static method to close the factory when application shuts down
    public static void closeEntityManagerFactory() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            System.out.println("EntityManagerFactory closed");
        }
    }

    public void updateLastLoginTime(int userId) {
        executeInTransaction(() -> {
            em.createQuery("UPDATE User u SET u.lastLoginAt = :now WHERE u.id = :id")
                    .setParameter("now", LocalDateTime.now())
                    .setParameter("id", userId)
                    .executeUpdate();
        });
    }
}