package il.cshaifasweng.OCSFMediatorExample.server;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;


public class DataManager {
    public static final SessionFactory factory = Database.getSessionFactoryInstance();

    public static <T> void add(T entity)
    {
        Session session = factory.openSession();
        session.beginTransaction();
        session.save(entity);
        session.getTransaction().commit();
        session.close();
    }

    public static <T> void delete(T entity)
    {
        Session session = factory.openSession();
        session.beginTransaction();
        session.delete(entity);
        session.getTransaction().commit();
        session.close();
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
