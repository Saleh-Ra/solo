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
        Database.getSessionFactoryInstance();
        DatabaseInitializer.initializeAll();  // 👈 Add this line
        System.out.println("hello");
        server.listen();
    }
}

//<property name="hibernate.hbm2ddl.auto">update</property>
//DatabaseInitializer db = new DatabaseInitializer();