package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;
import java.util.Scanner;

public class Database {
    private static SessionFactory sessionFactory;
    //private static final String password = "212101828";

    static {
        sessionFactory = getSessionFactory();
    }

    private static SessionFactory getSessionFactory() throws HibernateException {
        if (sessionFactory == null) {
            Configuration configuration = new Configuration();
            Scanner scanner = new Scanner(System.in);

            // Prompt the user
            System.out.print("Enter a password: ");

            // Read a string input
            String password = scanner.nextLine();
            // Add the necessary properties for connecting to the database
            configuration.setProperty("hibernate.connection.url", "jdbc:mysql://localhost:3306/myfirstdatabase?serverTimezone=UTC");
            configuration.setProperty("hibernate.connection.username", "root");
            configuration.setProperty("hibernate.connection.password", password);
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            configuration.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
            configuration.setProperty("hibernate.hbm2ddl.auto", "update"); // Auto-create or update table

            // Add the entity classes
            configuration.addAnnotatedClass(MenuItem.class);
            configuration.addAnnotatedClass(Branch.class);
            configuration.addAnnotatedClass(BranchManager.class);
            configuration.addAnnotatedClass(Client.class);
            configuration.addAnnotatedClass(Complaint.class);
            configuration.addAnnotatedClass(Delivery.class);
            configuration.addAnnotatedClass(Menu.class);
            configuration.addAnnotatedClass(Order.class);
            configuration.addAnnotatedClass(Payment.class);
            configuration.addAnnotatedClass(ResturantChain.class);
            configuration.addAnnotatedClass(Reservation.class);
            configuration.addAnnotatedClass(ResturantChainManager.class);
            configuration.addAnnotatedClass(ResturantTable.class);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();

            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        }
        return sessionFactory;
    }

    public static SessionFactory getSessionFactoryInstance() {
        return sessionFactory;
    }


    // Optional shutdown method if needed
    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    public void initializeMenu() {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<MenuItem> query = builder.createQuery(MenuItem.class);
            query.from(MenuItem.class);
            List<MenuItem> existingItems = session.createQuery(query).getResultList();

            if (existingItems.isEmpty()) {
                // Add default menu items
                session.save(new MenuItem("Pizza", "Cheese, Tomato, Onions, Mushroom", "Vegetarian", 500.00));
                session.save(new MenuItem("Hamburger", "Beef, Lettuce, Tomato", "No Cheese", 65.00));
                session.save(new MenuItem("Vegan Hamburger", "Vegan patty, Tomato, Pickles, Lettuce", "Vegan", 60.00));
                session.save(new MenuItem("SOUR CREAM SPINACH PASTA", "Sour cream, Garlic, Spinach", "Gluten-Free", 55.00));
                session.save(new MenuItem("CEASAR SALAD", "Lettuce, Chicken breast, Parmesan cheese, Onions", "Keto-Friendly", 60.00));
            }

            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Failed to initialize the menu: " + e.getMessage());
        }
    }


    public void addMenuItem(MenuItem item) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.save(item);
            session.getTransaction().commit();
            //getMenuItems();
        } catch (Exception e) {
            System.err.println("Failed to add menu item: " + e.getMessage());
        }
    }

    public void updatePriceByName(String name, double newPrice) {
        try (Session session = sessionFactory.openSession()) {
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

    public void deleteMenuItem(int id) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            MenuItem item = session.get(MenuItem.class, id);
            if (item != null) {
                session.delete(item);
                session.getTransaction().commit();
                //getMenuItems();
            } else {
                System.err.println("No menu item found with ID: " + id);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete menu item: " + e.getMessage());
        }
    }

    public List<MenuItem> getMenuItems() {
        List<MenuItem> items = null;
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<MenuItem> query = builder.createQuery(MenuItem.class);
            query.from(MenuItem.class);
            items = session.createQuery(query).getResultList();
            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Failed to get menu items: " + e.getMessage());
        }
        return items;
    }
}
