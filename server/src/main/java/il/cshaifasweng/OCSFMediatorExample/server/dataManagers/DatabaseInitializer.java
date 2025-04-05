package il.cshaifasweng.OCSFMediatorExample.server.dataManagers;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;


public class DatabaseInitializer {

    public static final SessionFactory factory = Database.getSessionFactoryInstance();
    
    public static void initializeAll() {
        try {
            System.out.println("\n=== Starting database initialization ===\n");
            
            // Check if database has already been initialized
            if (isDatabaseInitialized()) {
                System.out.println("Database already contains data. Skipping initialization.");
                return;
            }
            
            System.out.println("Database is empty. Starting fresh initialization...");
            
            // Initialize in the correct order to avoid circular dependencies
            initializeRestaurantTable();  // First tables
            System.out.println("\n✓ Restaurant tables initialized\n");
            
            initializeMenu();             // Then menu items
            System.out.println("\n✓ Menu initialized\n");
            
            initializeRestaurantChain();  // Then restaurant chain
            System.out.println("\n✓ Restaurant chain initialized\n");
            
            initializeBranches();         // Then branches (which need chain and tables)
            System.out.println("\n✓ Branches initialized\n");
            
            initializeClientsAndAccounts();  // Then clients and their accounts
            System.out.println("\n✓ Clients initialized\n");
            
            initializeManagers();            // Finally managers
            System.out.println("\n✓ Managers initialized\n");
            
            System.out.println("\n=== Database initialization completed successfully! ===\n");
            
            // Verify final state
            try (Session session = factory.openSession()) {
                long menuItems = session.createQuery("select count(*) from MenuItem", Long.class).getSingleResult();
                long users = session.createQuery("select count(*) from UserAccount", Long.class).getSingleResult();
                System.out.println("Final database state - Menu Items: " + menuItems + ", Users: " + users);
            }
        } catch (Exception e) {
            System.err.println("Error during database initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if the database already has data
     */
    private static boolean isDatabaseInitialized() {
        try (Session session = factory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            
            // Check if menu items exist
            CriteriaQuery<Long> menuCount = builder.createQuery(Long.class);
            menuCount.select(builder.count(menuCount.from(MenuItem.class)));
            long menuItems = session.createQuery(menuCount).getSingleResult();
            
            // Check if users exist
            CriteriaQuery<Long> userCount = builder.createQuery(Long.class);
            userCount.select(builder.count(userCount.from(UserAccount.class)));
            long users = session.createQuery(userCount).getSingleResult();
            
            System.out.println("Database check - Menu Items: " + menuItems + ", Users: " + users);
            
            // Only consider database initialized if both menu items and users exist
            return menuItems > 0 && users > 0;
        } catch (Exception e) {
            System.err.println("Error checking if database is initialized: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Checks if a user with the given phone number already exists
     */
    private static boolean userExists(Session session, String phoneNumber) {
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<UserAccount> query = builder.createQuery(UserAccount.class);
        Root<UserAccount> root = query.from(UserAccount.class);
        query.select(root).where(builder.equal(root.get("phoneNumber"), phoneNumber));
        
        List<UserAccount> results = session.createQuery(query).getResultList();
        return !results.isEmpty();
    }

    public static void initializeClientsAndAccounts() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();
            
            // Sample clients to add
            String[][] clientData = {
                {"Saleh2", "05281890992", "saleh1234"},
                {"Alex", "0528189091", "alex1234"},
                {"Maria", "0528189093", "maria1234"}
            };
            
            for (String[] data : clientData) {
                // Only add if phone number doesn't exist
                if (!userExists(session, data[1])) {
                    UserAccount account = new UserAccount(data[0], data[1], "client", data[2]);
                    session.save(account);
                    
                    Client client = new Client(data[0], account);
                    session.save(client);
                    
                    System.out.println("Added client: " + data[0]);
                } else {
                    System.out.println("Client with phone " + data[1] + " already exists, skipped.");
                }
            }

            session.getTransaction().commit();
            System.out.println("Client initialization completed");
        } catch (Exception e) {
            System.err.println("Failed to initialize clients: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void initializeMenu() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<MenuItem> query = builder.createQuery(MenuItem.class);
            query.from(MenuItem.class);
            List<MenuItem> existingItems = session.createQuery(query).getResultList();

            if (existingItems.isEmpty()) {
                System.out.println("Creating menu items...");

                // Create menu items with more descriptive logging
                MenuItem[] menuItems = {
                    new MenuItem("Pizza", "Cheese, Tomato, Onions, Mushroom", "Vegetarian", 500.00),
                    new MenuItem("Hamburger", "Beef, Lettuce, Tomato", "No Cheese", 65.00),
                    new MenuItem("Vegan Hamburger", "Vegan patty, Tomato, Pickles, Lettuce", "Vegan", 60.00),
                    new MenuItem("SOUR CREAM SPINACH PASTA", "Sour cream, Garlic, Spinach", "Gluten-Free", 55.00),
                    new MenuItem("CEASAR SALAD", "Lettuce, Chicken breast, Parmesan cheese, Onions", "Keto-Friendly", 60.00)
                };

                for (MenuItem item : menuItems) {
                    try {
                        System.out.println("Saving menu item: " + item.getName());
                        session.save(item);
                        System.out.println("Successfully saved menu item: " + item.getName());
                    } catch (Exception e) {
                        System.err.println("Error saving menu item " + item.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                session.flush(); // Force the session to flush changes to the database
            }

            session.getTransaction().commit();
            
            // Verify the items were saved
            List<MenuItem> savedItems = session.createQuery("from MenuItem", MenuItem.class).getResultList();
            System.out.println("Menu initialization completed. Total items in database: " + savedItems.size());
            for (MenuItem item : savedItems) {
                System.out.println("Saved item: " + item.getName() + " (ID: " + item.getId() + ")");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize the menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void initializeRestaurantChain() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<RestaurantChain> query = builder.createQuery(RestaurantChain.class);
            query.from(RestaurantChain.class);
            List<RestaurantChain> existingChains = session.createQuery(query).getResultList();

            if (existingChains.isEmpty()) {
                System.out.println("Creating restaurant chain...");
                RestaurantChain chain = new RestaurantChain("foreign restaurant", null);
                session.save(chain);
            }

            session.getTransaction().commit();
            System.out.println("Restaurant chain initialization completed");
        } catch (Exception e) {
            System.err.println("Failed to initialize the chain: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void initializeBranches() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Branch> query = builder.createQuery(Branch.class);
            query.from(Branch.class);
            List<Branch> existingBranches = session.createQuery(query).getResultList();

            if (existingBranches.isEmpty()) {
                System.out.println("Creating branches...");
                
                // Get the restaurant chain
                List<RestaurantChain> chains = session.createQuery("from RestaurantChain where name = 'foreign restaurant'").getResultList();
                
                if (chains.isEmpty()) {
                    System.out.println("No restaurant chain found, creating one...");
                    RestaurantChain chain = new RestaurantChain("foreign restaurant", null);
                    session.save(chain);
                    
                    // Create branches with tables
                    Branch branch1 = new Branch("Tel-Aviv", "daily 10:00-22:00", chain, null);
                    session.save(branch1);
                    
                    Branch branch2 = new Branch("Haifa", "from Sunday to Thursday 10:00-22:00", chain, null);
                    session.save(branch2);
                    
                    Branch branch3 = new Branch("Jerusalem", "daily 10:00-22:00", chain, null);
                    session.save(branch3);
                    
                    Branch branch4 = new Branch("Beer-Shiva", "daily 10:00-22:00", chain, null);
                    session.save(branch4);
                } else {
                    RestaurantChain chain = chains.get(0);
                    
                    // Create branches with tables
                    Branch branch1 = new Branch("Tel-Aviv", "daily 10:00-22:00", chain, null);
                    session.save(branch1);
                    
                    Branch branch2 = new Branch("Haifa", "from Sunday to Thursday 10:00-22:00", chain, null);
                    session.save(branch2);
                    
                    Branch branch3 = new Branch("Jerusalem", "daily 10:00-22:00", chain, null);
                    session.save(branch3);
                    
                    Branch branch4 = new Branch("Beer-Shiva", "daily 10:00-22:00", chain, null);
                    session.save(branch4);
                }
            }

            session.getTransaction().commit();
            System.out.println("Branch initialization completed");
        } catch (Exception e) {
            System.err.println("Failed to initialize the branches: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void initializeRestaurantTable() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<RestaurantTable> query = builder.createQuery(RestaurantTable.class);
            query.from(RestaurantTable.class);
            List<RestaurantTable> existingTables = session.createQuery(query).getResultList();

            if (existingTables.isEmpty()) {
                System.out.println("Creating restaurant tables...");
                
                // Create tables for different areas
                for (int i = 1; i <= 12; i++) {
                    session.save(new RestaurantTable(3 + (i % 2), 0, "Main", 
                        (i % 3 == 0) ? "near bar" : (i % 3 == 1) ? "center" : "near kitchen", 
                        "Table " + i, new boolean[720]));
                }
                
                for (int i = 13; i <= 18; i++) {
                    session.save(new RestaurantTable(2 + (i % 3), 0, "Bar", 
                        (i % 3 == 0) ? "quiet" : (i % 3 == 1) ? "window" : "center", 
                        "Table " + i, new boolean[720]));
                }
                
                for (int i = 19; i <= 24; i++) {
                    session.save(new RestaurantTable(2 + (i % 3), 0, "Out", 
                        (i % 3 == 0) ? "cozy" : (i % 3 == 1) ? "window" : "quiet", 
                        "Table " + i, new boolean[720]));
                }
                
                for (int i = 25; i <= 30; i++) {
                    session.save(new RestaurantTable(2 + (i % 3), 0, "Inside", 
                        (i % 4 == 0) ? "center" : (i % 4 == 1) ? "quiet" : (i % 4 == 2) ? "window" : "near kitchen", 
                        "Table " + i, new boolean[720]));
                }
            }

            session.getTransaction().commit();
            System.out.println("Restaurant table initialization completed with " + (existingTables.isEmpty() ? "new tables" : "existing tables"));
        } catch (Exception e) {
            System.err.println("Failed to initialize restaurant tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void initializeManagers() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();
            
            // Manager data (name, phone, password)
            String[][] managerData = {
                {"Saleh", "0528189099", "saleh123"},
                {"Nassim", "0544347642", "Nassim123"},
                {"Hali", "0526112238", "Hali123"},
                {"Mohammad", "0543518310", "Mohammad123"},
                {"Natali", "0502404146", "Natali123"}
            };
            
            // First add the chain manager if not exists
            if (!userExists(session, managerData[0][1])) {
                UserAccount chainManagerAccount = new UserAccount(managerData[0][0], managerData[0][1], "manager", managerData[0][2]);
                session.save(chainManagerAccount);
                session.save(new RestaurantChainManager(managerData[0][0], chainManagerAccount));
                System.out.println("Added chain manager: " + managerData[0][0]);
            } else {
                System.out.println("Chain manager with phone " + managerData[0][1] + " already exists, skipped.");
            }
            
            // Branch managers
            List<Branch> branches = session.createQuery("from Branch").getResultList();
            
            if (!branches.isEmpty()) {
                for (int i = 0; i < Math.min(branches.size(), managerData.length - 1); i++) {
                    int idx = i + 1; // Skip the first manager (chain manager)
                    
                    if (!userExists(session, managerData[idx][1])) {
                        UserAccount account = new UserAccount(managerData[idx][0], managerData[idx][1], "manager", managerData[idx][2]);
                        session.save(account);
                        session.save(new BranchManager(managerData[idx][0], branches.get(i), account));
                        System.out.println("Added branch manager: " + managerData[idx][0]);
                    } else {
                        System.out.println("Branch manager with phone " + managerData[idx][1] + " already exists, skipped.");
                    }
                }
            }

            session.getTransaction().commit();
            System.out.println("Manager initialization completed");
        } catch (Exception e) {
            System.err.println("Failed to initialize managers: " + e.getMessage());
            e.printStackTrace();
        }
    }
}