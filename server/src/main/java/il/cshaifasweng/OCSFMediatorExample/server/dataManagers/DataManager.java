package il.cshaifasweng.OCSFMediatorExample.server.dataManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.Database;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class DataManager {
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

    // Delete any entity
    public static <T> void delete(T entity) {
        Session session = factory.openSession();
        session.beginTransaction();
        session.delete(entity);
        session.getTransaction().commit();
        session.close();
    }

    // Fetch all entries of a specific type
    public static <T> List<T> fetchAll(Class<T> entityType) {
        Session session = factory.openSession();
        session.beginTransaction();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityType);
        query.from(entityType);
        List<T> results = session.createQuery(query).getResultList();

        session.getTransaction().commit();
        session.close();

        return results;
    }

    public static List<Order> fetchOrdersByPhoneNumber(String phoneNumber) {
        System.out.println("Fetching orderss by phone number: " + phoneNumber);
        Session session = factory.openSession();
        session.beginTransaction();

        String hql = "FROM Order c WHERE c.phoneNumber = :phoneNumber";
        List<Order> results = session.createQuery(hql, Order.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList();

        session.getTransaction().commit();
        session.close();

        System.out.println("Found " + results.size() + " orderss with phone number: " + phoneNumber);
        return results;
    }


    // Special method for fetching clients by phone number
    public static List<Client> fetchClientsByPhoneNumber(String phoneNumber) {
        System.out.println("Fetching clients by phone number: " + phoneNumber);
        Session session = factory.openSession();
        session.beginTransaction();

        String hql = "FROM Client c WHERE c.account.phoneNumber = :phoneNumber";
        List<Client> results = session.createQuery(hql, Client.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList();

        session.getTransaction().commit();
        session.close();
        
        System.out.println("Found " + results.size() + " clients with phone number: " + phoneNumber);
        return results;
    }

    // Method to fetch user accounts directly by phone number
    public static List<UserAccount> fetchUserAccountsByPhoneNumber(String phoneNumber) {
        System.out.println("Fetching user accounts by phone number: " + phoneNumber);
        Session session = factory.openSession();
        session.beginTransaction();

        String hql = "FROM UserAccount ua WHERE ua.phoneNumber = :phoneNumber";
        List<UserAccount> results = session.createQuery(hql, UserAccount.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList();

        session.getTransaction().commit();
        session.close();
        
        System.out.println("Found " + results.size() + " user accounts with phone number: " + phoneNumber);
        return results;
    }

    // Fetch by a specific field (like name, category, phoneNumber, etc.)
    public static <T> List<T> fetchByField(Class<T> entityType, String fieldName, Object value) {
        Session session = factory.openSession();
        session.beginTransaction();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityType);
        var root = query.from(entityType);
        query.select(root).where(builder.equal(root.get(fieldName), value));

        List<T> results = session.createQuery(query).getResultList();

        session.getTransaction().commit();
        session.close();

        return results;
    }

    public static <T> List<T> fetchByIdRange(Class<T> entityType, int minId, int maxId) {
        Session session = factory.openSession();
        session.beginTransaction();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityType);
        Root<T> root = query.from(entityType);
        query.select(root).where(
                builder.between(root.get("id"), minId, maxId)
        );

        List<T> results = session.createQuery(query).getResultList();

        session.getTransaction().commit();
        session.close();
        return results;
    }


    // Update one field by condition (e.g. set price = 60 where name = "Pizza")
    public static <T> int updateFieldByCondition(Class<T> entityType, String fieldToUpdate, Object newValue, String conditionField, Object conditionValue) {
        Session session = factory.openSession();
        session.beginTransaction();

        String hql = "UPDATE " + entityType.getSimpleName() +
                " SET " + fieldToUpdate + " = :newValue" +
                " WHERE " + conditionField + " = :conditionValue";

        int updatedEntities = session.createQuery(hql)
                .setParameter("newValue", newValue)
                .setParameter("conditionValue", conditionValue)
                .executeUpdate();

        session.getTransaction().commit();
        session.close();

        return updatedEntities;
    }
}
