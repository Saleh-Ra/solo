package il.cshaifasweng.OCSFMediatorExample.server.dataManagers;

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
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
            configuration.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
            configuration.setProperty("hibernate.hbm2ddl.auto", "create"); // Temporarily changed to create-drop to recreate tables
            configuration.setProperty("hibernate.show_sql", "true");
            configuration.setProperty("hibernate.format_sql", "true");
            configuration.setProperty("hibernate.use_sql_comments", "true");

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
            configuration.addAnnotatedClass(RestaurantChain.class);
            configuration.addAnnotatedClass(Reservation.class);
            configuration.addAnnotatedClass(RestaurantChainManager.class);
            configuration.addAnnotatedClass(RestaurantTable.class);
            configuration.addAnnotatedClass(UserAccount.class);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();

            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
            System.out.println("Session factory created successfully");
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
}
