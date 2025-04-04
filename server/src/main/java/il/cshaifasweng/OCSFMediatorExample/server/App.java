package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.Database;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DatabaseInitializer;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
	
	private static SimpleServer server;
    public static void main( String[] args ) throws IOException
    {
        server = new SimpleServer(3000);
        
        try {
            System.out.println("Initializing database connection...");
            Database.getSessionFactoryInstance();
            System.out.println("Database connection established.");
            
            System.out.println("Initializing database data...");
            DatabaseInitializer.initializeAll();
            System.out.println("Database initialization complete.");
        } catch (Exception e) {
            System.err.println("ERROR: Database initialization failed!");
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("Starting server on port 3000...");
        server.listen();
        System.out.println("Server is listening for connections.");
    }
}

//<property name="hibernate.hbm2ddl.auto">update</property>
//DatabaseInitializer db = new DatabaseInitializer();