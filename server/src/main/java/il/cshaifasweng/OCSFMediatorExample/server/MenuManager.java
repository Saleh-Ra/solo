package il.cshaifasweng.OCSFMediatorExample.server;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class MenuManager {
    public static final SessionFactory factory = Database.getSessionFactoryInstance();


    static public void updatePriceByName(String name, double newPrice) {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            String hql = "UPDATE MenuItem SET price = :newPrice WHERE name = :name";
            int updatedEntities = session.createQuery(hql)
                    .setParameter("newPrice", newPrice)
                    .setParameter("name", name)
                    .executeUpdate();

            session.getTransaction().commit();
            //getMenuItems();
            if (updatedEntities == 0) {
                System.err.println("No menu item found with name: " + name);
            } else {
                System.out.println("Price updated for: " + name);
            }
        } catch (Exception e) {
            System.err.println("Failed to update price: " + e.getMessage());
        }
    }
}
