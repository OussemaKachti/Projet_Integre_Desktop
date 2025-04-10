// Path: src/main/java/services/UserService.java
package services;

import java.util.List;
import java.util.Set;

import entities.User;
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

public class UserService implements Service<User> {

    private final EntityManagerFactory emf;
    private final EntityManager em;

    public UserService() {
        try {
            emf = Persistence.createEntityManagerFactory("uniclubsPU");
            em = emf.createEntityManager();
            System.out.println("EntityManager created successfully");
        } catch (PersistenceException e) {
            System.err.println("Failed to initialize persistence: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

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
                        "Validation Error: " + v.getPropertyPath() + " " + v.getMessage()
                    );
                }
                throw new ConstraintViolationException(violations);
            }

            // Proceed with persistence
            executeInTransaction(() -> {
                em.persist(user);
                em.flush();  // Force immediate insert to verify operation
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
        executeInTransaction(() -> em.merge(user));
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
            User.class
        );
        query.setParameter("keyword", "%" + keyword + "%");
        return query.getResultList();
    }
    
    public User findByEmail(String email) {
        try {
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.email = :email", 
                User.class
            );
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

    public void close() {
        if (em != null && em.isOpen()) em.close();
        if (emf != null && emf.isOpen()) emf.close();
    }
}