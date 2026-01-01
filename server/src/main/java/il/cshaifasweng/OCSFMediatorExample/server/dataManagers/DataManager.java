package il.cshaifasweng.OCSFMediatorExample.server.dataManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/*
 * THREAD-SAFE DATA MANAGER:
 * =========================
 * 
 * This class is thread-safe because:
 * 1. SessionFactory is thread-safe (shared across all threads)
 * 2. Each method creates its own Session (thread-local, not shared)
 * 3. Sessions are properly closed after use
 * 
 * This is the correct pattern for multi-threaded Hibernate applications.
 */
public class DataManager {
    // SessionFactory is thread-safe and can be shared across threads
    public static final SessionFactory factory = Database.getSessionFactoryInstance();

    // Add any entity
    public static <T> void add(T entity) {
        System.out.println("\n=== Database Add Operation ===");
        System.out.println("Entity type: " + entity.getClass().getSimpleName());
        System.out.println("Entity details: " + entity);
        
        Session session = null;
        try {
            System.out.println("Opening database session...");
            session = factory.openSession();
            System.out.println("Session opened successfully");
            
            System.out.println("Beginning transaction...");
            session.beginTransaction();
            System.out.println("Transaction begun");
            
            System.out.println("Saving entity to session...");
            session.save(entity);
            System.out.println("Entity saved to session");
            
            System.out.println("Committing transaction...");
            session.getTransaction().commit();
            System.out.println("Transaction committed successfully");
            
            System.out.println("Entity saved successfully to database");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to save entity: " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                System.err.println("Rolling back transaction...");
                session.getTransaction().rollback();
                System.err.println("Transaction rolled back due to error");
            }
        } finally {
            if (session != null && session.isOpen()) {
                System.out.println("Closing session...");
                session.close();
                System.out.println("Session closed");
            }
        }
        System.out.println("=== End Database Add Operation ===\n");
    }

    // Delete any entity (thread-safe)
    public static <T> void delete(T entity) {
        Session session = null;
        try {
            session = factory.openSession();
            session.beginTransaction();
            session.delete(entity);
            session.getTransaction().commit();
            System.out.println("Entity deleted successfully: " + entity.getClass().getSimpleName());
        } catch (Exception e) {
            System.err.println("ERROR: Failed to delete entity: " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // Fetch all entries of a specific type (thread-safe)
    public static <T> List<T> fetchAll(Class<T> entityType) {
        Session session = null;
        try {
            session = factory.openSession();
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<T> query = builder.createQuery(entityType);
            query.from(entityType);
            List<T> results = session.createQuery(query).getResultList();

            session.getTransaction().commit();
            return results;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch all entities of type " + entityType.getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // Fetch orders by phone number (thread-safe)
    public static List<Order> fetchOrdersByPhoneNumber(String phoneNumber) {
        System.out.println("Fetching orders by phone number: " + phoneNumber);
        Session session = null;
        try {
            session = factory.openSession();
            session.beginTransaction();

            String hql = "FROM Order c WHERE c.phoneNumber = :phoneNumber";
            List<Order> results = session.createQuery(hql, Order.class)
                    .setParameter("phoneNumber", phoneNumber)
                    .getResultList();

            session.getTransaction().commit();
            System.out.println("Found " + results.size() + " orders with phone number: " + phoneNumber);
            return results;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch orders: " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // Fetch clients by phone number (thread-safe)
    public static List<Client> fetchClientsByPhoneNumber(String phoneNumber) {
        System.out.println("Fetching clients by phone number: " + phoneNumber);
        Session session = null;
        try {
            session = factory.openSession();
            session.beginTransaction();

            String hql = "FROM Client c WHERE c.account.phoneNumber = :phoneNumber";
            List<Client> results = session.createQuery(hql, Client.class)
                    .setParameter("phoneNumber", phoneNumber)
                    .getResultList();

            session.getTransaction().commit();
            System.out.println("Found " + results.size() + " clients with phone number: " + phoneNumber);
            return results;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch clients: " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // Fetch user accounts by phone number (thread-safe)
    public static List<UserAccount> fetchUserAccountsByPhoneNumber(String phoneNumber) {
        System.out.println("Fetching user accounts by phone number: " + phoneNumber);
        Session session = null;
        try {
            session = factory.openSession();
            session.beginTransaction();

            String hql = "FROM UserAccount ua WHERE ua.phoneNumber = :phoneNumber";
            List<UserAccount> results = session.createQuery(hql, UserAccount.class)
                    .setParameter("phoneNumber", phoneNumber)
                    .getResultList();

            session.getTransaction().commit();
            System.out.println("Found " + results.size() + " user accounts with phone number: " + phoneNumber);
            return results;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch user accounts: " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // Fetch by a specific field (thread-safe)
    public static <T> List<T> fetchByField(Class<T> entityType, String fieldName, Object value) {
        Session session = null;
        try {
            session = factory.openSession();
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<T> query = builder.createQuery(entityType);
            var root = query.from(entityType);
            query.select(root).where(builder.equal(root.get(fieldName), value));

            List<T> results = session.createQuery(query).getResultList();

            session.getTransaction().commit();
            return results;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch by field " + fieldName + ": " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // Fetch by ID range (thread-safe)
    public static <T> List<T> fetchByIdRange(Class<T> entityType, int minId, int maxId) {
        Session session = null;
        try {
            session = factory.openSession();
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<T> query = builder.createQuery(entityType);
            Root<T> root = query.from(entityType);
            query.select(root).where(
                    builder.between(root.get("id"), minId, maxId)
            );

            List<T> results = session.createQuery(query).getResultList();

            session.getTransaction().commit();
            return results;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch by ID range: " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // Update one field by condition (thread-safe)
    // e.g. set price = 60 where name = "Pizza"
    public static <T> int updateFieldByCondition(Class<T> entityType, String fieldToUpdate, Object newValue, String conditionField, Object conditionValue) {
        Session session = null;
        try {
            session = factory.openSession();
            session.beginTransaction();

            String hql = "UPDATE " + entityType.getSimpleName() +
                    " SET " + fieldToUpdate + " = :newValue" +
                    " WHERE " + conditionField + " = :conditionValue";

            int updatedEntities = session.createQuery(hql)
                    .setParameter("newValue", newValue)
                    .setParameter("conditionValue", conditionValue)
                    .executeUpdate();

            session.getTransaction().commit();
            System.out.println("Updated " + updatedEntities + " entities");
            return updatedEntities;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to update field: " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            return 0;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
