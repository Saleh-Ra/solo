package il.cshaifasweng.OCSFMediatorExample.server;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

public class DataManager {
    public static final SessionFactory factory = Database.getSessionFactoryInstance();

    public static <T> void add(T entity) {
        Session session = factory.openSession();
        session.beginTransaction();
        session.save(entity);
        session.getTransaction().commit();
        session.close();
    }

    public static <T> void save(T entity) {
        // Alias for 'add' to match usage in other parts of the code
        add(entity);
    }

    public static <T> void delete(T entity) {
        Session session = factory.openSession();
        session.beginTransaction();
        session.delete(entity);
        session.getTransaction().commit();
        session.close();
    }

    public static <T> T find(Class<T> entityType, int id) {
        Session session = factory.openSession();
        session.beginTransaction();
        T entity = session.get(entityType, id);
        session.getTransaction().commit();
        session.close();
        return entity;
    }

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
}
