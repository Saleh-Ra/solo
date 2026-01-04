package il.cshaifasweng.OCSFMediatorExample.server.dataManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

public class Database {
    private static SessionFactory sessionFactory;

    static {
        sessionFactory = getSessionFactory();
    }

    private static SessionFactory getSessionFactory() throws HibernateException {
        if (sessionFactory == null) {
            Configuration configuration = new Configuration();
            
            // Try to read password from properties file, environment variable, or prompt user
            String password = getDatabasePassword();
            
            // Add the necessary properties for connecting to the database
            configuration.setProperty("hibernate.connection.url", "jdbc:mysql://localhost:3306/myfirstdatabase?serverTimezone=UTC");
            configuration.setProperty("hibernate.connection.username", "root");
            configuration.setProperty("hibernate.connection.password", password);
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
            configuration.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
            configuration.setProperty("hibernate.hbm2ddl.auto", "update"); // Update mode: only modifies schema if entity definitions change
            configuration.setProperty("hibernate.show_sql", "true");
            configuration.setProperty("hibernate.format_sql", "true");
            configuration.setProperty("hibernate.use_sql_comments", "true");

            // Add the entity classes
            configuration.addAnnotatedClass(MenuItem.class);
            configuration.addAnnotatedClass(Branch.class);
            configuration.addAnnotatedClass(BranchManager.class);
            configuration.addAnnotatedClass(Client.class);
            configuration.addAnnotatedClass(Complaint.class);
            configuration.addAnnotatedClass(CustomerService.class);
            configuration.addAnnotatedClass(GeneralMessage.class);
            configuration.addAnnotatedClass(Delivery.class);
            configuration.addAnnotatedClass(Menu.class);
            configuration.addAnnotatedClass(Order.class);
            configuration.addAnnotatedClass(Payment.class);
            configuration.addAnnotatedClass(RestaurantChain.class);
            configuration.addAnnotatedClass(Reservation.class);
            configuration.addAnnotatedClass(RestaurantChainManager.class);
            configuration.addAnnotatedClass(RestaurantTable.class);
            configuration.addAnnotatedClass(UserAccount.class);
            configuration.addAnnotatedClass(Worker.class);

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

    /**
     * Gets the database password from properties file, environment variable, or prompts the user.
     * Priority: 1) database.properties file, 2) DB_PASSWORD environment variable, 3) user prompt
     */
    private static String getDatabasePassword() {
        // First, try to read from database.properties file (bundled with JAR)
        try {
            InputStream inputStream = Database.class.getClassLoader().getResourceAsStream("database.properties");
            if (inputStream != null) {
                Properties properties = new Properties();
                properties.load(inputStream);
                String password = properties.getProperty("db.password");
                if (password != null && !password.trim().isEmpty()) {
                    System.out.println("Database password loaded from database.properties");
                    inputStream.close();
                    return password.trim();
                }
                inputStream.close();
            }
        } catch (Exception e) {
            System.out.println("Could not read database.properties file: " + e.getMessage());
        }

        // Second, try to read from environment variable
        String envPassword = System.getenv("DB_PASSWORD");
        if (envPassword != null && !envPassword.trim().isEmpty()) {
            System.out.println("Database password loaded from DB_PASSWORD environment variable");
            return envPassword.trim();
        }

        // Last resort: prompt the user
        System.out.print("Enter database password: ");
        try (Scanner scanner = new Scanner(System.in)) {
            return scanner.nextLine();
        }
    }
}
