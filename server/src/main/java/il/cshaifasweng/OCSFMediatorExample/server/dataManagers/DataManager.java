package il.cshaifasweng.OCSFMediatorExample.server.dataManagers;

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
        Session session = factory.openSession();
        session.beginTransaction();
        session.save(entity);
        session.getTransaction().commit();
        session.close();
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
