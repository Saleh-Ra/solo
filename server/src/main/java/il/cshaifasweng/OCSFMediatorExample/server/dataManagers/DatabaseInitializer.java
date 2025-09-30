package il.cshaifasweng.OCSFMediatorExample.server.dataManagers;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;


public class DatabaseInitializer {

    public static final SessionFactory factory = Database.getSessionFactoryInstance();
    
    private static final String[] CATEGORIES = {
        "First meal",
        "Drinks",
        "Main course",
        "Appetizer",
        "Desert",
        "Beverages"
    };
    
    public static void initializeAll() {
        try {
            System.out.println("\n=== Starting database initialization ===\n");
            
            // Check if database has already been initialized
            boolean databaseInitialized = isDatabaseInitialized();
            System.out.println("Database initialized check: " + databaseInitialized);
            
            if (!databaseInitialized) {
                System.out.println("Database is empty. Starting fresh initialization...");
                
                // Initialize in the correct order to avoid circular dependencies
                System.out.println("\n1. Initializing restaurant chain...");
                initializeRestaurantChain();
                System.out.println("✓ Restaurant chain initialized\n");
                
                System.out.println("2. Initializing branches...");
                initializeBranches();
                System.out.println("✓ Branches initialized\n");
                
                System.out.println("3. Initializing restaurant tables...");
                initializeRestaurantTable();
                System.out.println("✓ Restaurant tables initialized\n");
                
                System.out.println("4. Initializing default menu...");
                initializeMenu();
                System.out.println("✓ Default menu initialized\n");
                
                System.out.println("5. Initializing branch menus...");
                initializeBranchMenus();
                System.out.println("✓ Branch menus initialized\n");
                
                System.out.println("6. Initializing clients and accounts...");
                initializeClientsAndAccounts();
                System.out.println("✓ Clients initialized\n");
                
                System.out.println("7. Initializing managers...");
                initializeManagers();
                System.out.println("✓ Managers initialized\n");

                System.out.println("8. Initializing reservations...");
                initializeReservations();
                System.out.println("✓ Reservations initialized\n");
                
                System.out.println("9. Initializing orders...");
                initializeOrders();
                System.out.println("✓ Orders initialized\n");
            } else {
                System.out.println("Database already contains basic data. Checking for new roles and branch menus...");
                
                // Force check branch menus - this will ensure menus are created even for existing database
                ensureBranchMenusExist();
            }
            
            // Always initialize new roles regardless of whether database is initialized
            // Check if we have any chain managers
            if (!roleExists("chain_manager")) {
                System.out.println("\nInitializing chain managers...");
                initializeChainManagers();
                System.out.println("✓ Chain managers initialized\n");
            } else {
                System.out.println("\n✓ Chain managers already exist\n");
            }
            
            // Check if we have any customer support
            if (!roleExists("customer_support")) {
                System.out.println("Initializing customer support...");
                initializeCustomerSupport();
                System.out.println("✓ Customer support initialized\n");
            } else {
                System.out.println("✓ Customer support already exists\n");
            }
            
            // Check if we have any nutritionists
            if (!roleExists("nutritionist")) {
                System.out.println("Initializing nutritionists...");
                initializeNutritionists();
                System.out.println("✓ Nutritionists initialized\n");
            } else {
                System.out.println("✓ Nutritionists already exist\n");
            }
            
            System.out.println("\n=== Database initialization completed successfully! ===\n");
            
            // Verify final state with more detailed logging
            try (Session session = factory.openSession()) {
                System.out.println("\nVerifying database state:");
                
                // Check menu items
                List<MenuItem> menuItems = session.createQuery("from MenuItem", MenuItem.class).getResultList();
                System.out.println("Menu Items: " + menuItems.size());
                for (MenuItem item : menuItems) {
                    System.out.println("  - " + item.getName() + " (ID: " + item.getId() + ", Category: " + item.getCategory() + ")");
                }
                
                // Check users
                List<UserAccount> users = session.createQuery("from UserAccount", UserAccount.class).getResultList();
                System.out.println("\nUsers: " + users.size());
                for (UserAccount user : users) {
                    System.out.println("  - " + user.getName() + " (ID: " + user.getId() + ", Role: " + user.getRole() + ")");
                }
                
                // Check branches
                List<Branch> branches = session.createQuery("from Branch", Branch.class).getResultList();
                System.out.println("\nBranches: " + branches.size());
                for (Branch branch : branches) {
                    System.out.println("  - " + branch.getLocation() + " (ID: " + branch.getId() + ")");
                }
                
                // Check tables
                List<RestaurantTable> tables = session.createQuery("from RestaurantTable", RestaurantTable.class).getResultList();
                System.out.println("\nTables: " + tables.size());
                for (RestaurantTable table : tables) {
                    System.out.println("  - " + table.getLabel() + " (ID: " + table.getid() + ", Location: " + table.getLocation() + ")");
                }
                
                // Check reservations
                List<Reservation> reservations = session.createQuery("from Reservation", Reservation.class).getResultList();
                System.out.println("\nReservations: " + reservations.size());
                for (Reservation reservation : reservations) {
                    System.out.println("  - ID: " + reservation.getId() + 
                                     ", Branch: " + reservation.getBranch().getLocation() +
                                     ", Time: " + reservation.getReservationTime());
                }
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
            // Check for menu items
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Long> menuQuery = builder.createQuery(Long.class);
            menuQuery.select(builder.count(menuQuery.from(MenuItem.class)));
            Long menuCount = session.createQuery(menuQuery).getSingleResult();
            
            // Check for user accounts
            CriteriaQuery<Long> userQuery = builder.createQuery(Long.class);
            userQuery.select(builder.count(userQuery.from(UserAccount.class)));
            Long userCount = session.createQuery(userQuery).getSingleResult();
            
            // The database is considered initialized if it has both menu items and user accounts
            boolean isInitialized = menuCount > 0 && userCount > 0;
            System.out.println("Database check: Found " + menuCount + " menu items and " + 
                             userCount + " user accounts. Database initialized: " + isInitialized);
            return isInitialized;
        } catch (Exception e) {
            System.err.println("Error checking database initialization status: " + e.getMessage());
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

                // Create menu items with categories
                MenuItem[] menuItems = {
                    new MenuItem("Pizza", "Cheese, Tomato, Onions, Mushroom", "Vegetarian", 500.00, "Main course"),
                    new MenuItem("Hamburger", "Beef, Lettuce, Tomato", "No Cheese", 65.00, "Main course"),
                    new MenuItem("Vegan Hamburger", "Vegan patty, Tomato, Pickles, Lettuce", "Vegan", 60.00, "Main course"),
                    new MenuItem("SOUR CREAM SPINACH PASTA", "Sour cream, Garlic, Spinach", "Gluten-Free", 55.00, "Main course"),
                    new MenuItem("CEASAR SALAD", "Lettuce, Chicken breast, Parmesan cheese, Onions", "Keto-Friendly", 60.00, "First meal"),
                    new MenuItem("Coca Cola", "Carbonated drink", "Regular", 12.00, "Beverages"),
                    new MenuItem("Ice Tea", "Black tea, Ice", "Sugar-free available", 10.00, "Beverages"),
                    new MenuItem("Garlic Bread", "Fresh bread, Garlic butter", "Vegetarian", 25.00, "Appetizer"),
                    new MenuItem("Chocolate Cake", "Dark chocolate, Cream", "Contains dairy", 35.00, "Desert"),
                    new MenuItem("Fresh Orange Juice", "Fresh oranges", "No sugar added", 15.00, "Drinks"),
                    new MenuItem("Greek Salad", "Cucumber, Tomatoes, Olives, Feta", "Vegetarian", 45.00, "First meal"),
                    new MenuItem("Tiramisu", "Coffee, Mascarpone, Ladyfingers", "Contains alcohol", 40.00, "Desert")
                };

                for (MenuItem item : menuItems) {
                    try {
                        // Set image paths for each menu item
                        setImagePathForMenuItem(item);
                        
                        System.out.println("Saving menu item: " + item.getName() + " (Category: " + item.getCategory() + ")");
                        session.save(item);
                        System.out.println("Successfully saved menu item: " + item.getName());
                    } catch (Exception e) {
                        System.err.println("Error saving menu item " + item.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                session.flush();
            }

            session.getTransaction().commit();
            
            // Verify the items were saved
            List<MenuItem> savedItems = session.createQuery("from MenuItem", MenuItem.class).getResultList();
            System.out.println("Menu initialization completed. Total items in database: " + savedItems.size());
            for (MenuItem item : savedItems) {
                System.out.println("Saved item: " + item.getName() + " (ID: " + item.getId() + ", Category: " + item.getCategory() + ")");
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
                    
                    Branch branch4 = new Branch("Beer-Sheva", "daily 10:00-22:00", chain, null);
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
                    
                    Branch branch4 = new Branch("Beer-Sheva", "daily 10:00-22:00", chain, null);
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
                
                // Get all branches
                List<Branch> branches = session.createQuery("from Branch", Branch.class).getResultList();
                if (branches.isEmpty()) {
                    System.out.println("No branches found. Please initialize branches first.");
                    return;
                }

                // Create tables for each branch
                for (Branch branch : branches) {
                    String location = branch.getLocation();
                    System.out.println("Creating tables for branch: " + location);
                    
                    // Create tables for different areas
                    for (int i = 1; i <= 8; i++) {
                        RestaurantTable table = new RestaurantTable(
                            3 + (i % 2),  // 3-4 seats
                            0,  // status
                            "Main",  // area
                            "Table " + i,  // label
                            new boolean[720]  // time slots
                        );
                        table.setLocation(location);  // Set the branch location
                        
                        // Link table to branch
                        List<RestaurantTable> branchTables = branch.getTables();
                        if (branchTables == null) {
                            branchTables = new ArrayList<>();
                        }
                        branchTables.add(table);
                        branch.setTables(branchTables);
                        
                        // Save the table first
                        session.save(table);
                        System.out.println("Created table " + i + " for " + location);
                    }
                    
                    // Update the branch with its tables
                    session.update(branch);
                }
                
                // Flush and commit the transaction
                session.flush();
                session.getTransaction().commit();
                
                // Verify the tables were created and linked
                try (Session verifySession = factory.openSession()) {
                    List<Branch> updatedBranches = verifySession.createQuery("from Branch", Branch.class).getResultList();
                    for (Branch branch : updatedBranches) {
                        System.out.println("Branch " + branch.getLocation() + " has " + 
                                         (branch.getTables() != null ? branch.getTables().size() : 0) + " tables");
                    }
                }
            } else {
                session.getTransaction().commit();
                System.out.println("Restaurant tables already exist");
            }
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
                {"Saleh", "0528189099", "Saleh123"},
                {"Nassim", "0544347642", "Nassim123"},
                {"Hali", "0526112238", "Hali123"},
                {"Mohammad", "0543518310", "Mohammad123"},
                {"Natali", "0502404146", "Natali123"}
            };
            
            // First add the chain manager if not exists - chain manager doesn't have branch
            if (!userExists(session, managerData[0][1])) {
                UserAccount chainManagerAccount = new UserAccount(managerData[0][0], managerData[0][1], "manager", managerData[0][2]);
                session.save(chainManagerAccount);
                session.save(new RestaurantChainManager(managerData[0][0], chainManagerAccount));
                System.out.println("Added chain manager: " + managerData[0][0]);
            } else {
                System.out.println("Chain manager with phone " + managerData[0][1] + " already exists, skipped.");
            }
            
            // Special case: Give Saleh (chain manager) access to Tel-Aviv branch as well
            if (userExists(session, managerData[0][1])) {
                List<Branch> telAvivBranches = session.createQuery("from Branch b where b.location = 'Tel-Aviv'").getResultList();
                if (!telAvivBranches.isEmpty()) {
                    Branch telAvivBranch = telAvivBranches.get(0);
                    List<UserAccount> salehAccounts = DataManager.fetchUserAccountsByPhoneNumber(managerData[0][1]);
                    if (!salehAccounts.isEmpty()) {
                        UserAccount salehAccount = salehAccounts.get(0);
                        // Update Saleh's account to include Tel-Aviv branch access
                        salehAccount.setBranchId(telAvivBranch.getId());
                        salehAccount.setBranchName(telAvivBranch.getLocation());
                        session.update(salehAccount);
                        System.out.println("Updated Saleh (chain manager) with Tel-Aviv branch access");
                    }
                }
            }
            
            // Create branch managers for the remaining managers (Nassim, Hali, Mohammad, Natali)
            List<Branch> branches = session.createQuery("from Branch").getResultList();
            
            if (!branches.isEmpty()) {
                for (int i = 0; i < Math.min(branches.size(), managerData.length - 1); i++) {
                    int idx = i + 1; // Skip the first manager (chain manager)
                    Branch branch = branches.get(i);
                    
                    if (!userExists(session, managerData[idx][1])) {
                        // Create the user account
                        UserAccount account = new UserAccount(
                            managerData[idx][0],  // name
                            managerData[idx][1],  // phone
                            "manager",            // role
                            managerData[idx][2]   // password
                        );
                        session.save(account);
                        
                        // Create the BranchManager
                        BranchManager branchManager = new BranchManager(managerData[idx][0], branch, account);
                        session.save(branchManager);
                        
                        // Set branch info in the user account
                        account.setBranchId(branch.getId());
                        account.setBranchName(branch.getLocation());
                        session.update(account);
                        
                        System.out.println("Added branch manager: " + managerData[idx][0] + " for branch: " + branch.getLocation());
                    } else {
                        System.out.println("Branch manager with phone " + managerData[idx][1] + " already exists, skipped.");
                    }
                }
            }

            session.getTransaction().commit();
            System.out.println("Manager initialization completed");
            
            // After initialization, update any managers that might still have null branch info
            updateExistingManagersWithBranchInfo();
        } catch (Exception e) {
            System.err.println("Failed to initialize managers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates existing manager accounts with branch information if not already set
     */
    public static void updateExistingManagersWithBranchInfo() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();
            
            // Get all managers with role='manager' and null branchId
            String hql = "FROM UserAccount u WHERE u.role = 'manager' AND (u.branchId IS NULL OR u.branchName IS NULL)";
            List<UserAccount> managersToUpdate = session.createQuery(hql, UserAccount.class).getResultList();
            
            if (managersToUpdate.isEmpty()) {
                System.out.println("No manager accounts need branch info update");
                session.getTransaction().commit();
                return;
            }
            
            System.out.println("Found " + managersToUpdate.size() + " manager accounts that need branch info update");
            
            // For each manager, find their branch through BranchManager relationship
            for (UserAccount manager : managersToUpdate) {
                String branchManagerHql = "FROM BranchManager bm WHERE bm.manager.id = :accountId";
                List<BranchManager> branchManagers = session.createQuery(branchManagerHql, BranchManager.class)
                    .setParameter("accountId", manager.getId())
                    .getResultList();
                    
                if (!branchManagers.isEmpty()) {
                    BranchManager branchManager = branchManagers.get(0);
                    Branch branch = branchManager.getBranch();
                    
                    if (branch != null) {
                        manager.setBranchId(branch.getId());
                        manager.setBranchName(branch.getLocation());
                        session.update(manager);
                        System.out.println("Updated manager " + manager.getName() + " with branch: " + branch.getLocation());
                    }
                }
            }
            
            session.getTransaction().commit();
            System.out.println("Finished updating manager accounts with branch info");
        } catch (Exception e) {
            System.err.println("Error updating managers with branch info: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add new initialization methods for chain managers (separate from initializeManagers)
    public static void initializeChainManagers() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();
            
            // Chain manager data (name, phone, password)
            String[][] chainManagerData = {
                {"David Cohen", "0521234567", "david123"},
                {"Sarah Levy", "0529876543", "sarah123"},
                {"Michael Gold", "0531234321", "michael123"}
            };
            
            // Add chain managers - they don't have branch associations
            for (String[] data : chainManagerData) {
                if (!userExists(session, data[1])) {
                    UserAccount account = new UserAccount(data[0], data[1], "chain_manager", data[2]);
                    // Branch fields intentionally left null for chain managers
                    session.save(account);
                    session.save(new RestaurantChainManager(data[0], account));
                    System.out.println("Added chain manager: " + data[0]);
                } else {
                    System.out.println("Chain manager with phone " + data[1] + " already exists, skipped.");
                }
            }

            session.getTransaction().commit();
            System.out.println("Chain manager initialization completed");
        } catch (Exception e) {
            System.err.println("Failed to initialize chain managers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void initializeCustomerSupport() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();
            
            // Get all branches to assign customer support staff
            List<Branch> branches = session.createQuery("from Branch").getResultList();
            
            if (branches.isEmpty()) {
                System.out.println("No branches found for customer support assignment");
                session.getTransaction().commit();
                return;
            }
            
            // Customer support data per branch (multiple per branch)
            String[][][] supportData = {
                // Tel-Aviv branch support staff
                {
                    {"Omer Tal", "0531111001", "omer123"},
                    {"Noa Golan", "0531111002", "noa123"}
                },
                // Haifa branch support staff
                {
                    {"Yair Cohen", "0532222001", "yair123"},
                    {"Maya Ben", "0532222002", "maya123"},
                    {"Tomer Levi", "0532222003", "tomer123"}
                },
                // Jerusalem branch support staff
                {
                    {"Shira Katz", "0533333001", "shira123"},
                    {"Dan Mizrahi", "0533333002", "dan123"}
                },
                // Beer-Sheva branch support staff
                {
                    {"Avi Peretz", "0534444001", "avi123"},
                    {"Dana Schwartz", "0534444002", "dana123"}
                }
            };
            
            // Create customer support accounts for each branch
            for (int i = 0; i < Math.min(branches.size(), supportData.length); i++) {
                Branch branch = branches.get(i);
                String[][] branchSupportData = supportData[i];
                
                for (String[] data : branchSupportData) {
                    if (!userExists(session, data[1])) {
                        UserAccount account = new UserAccount(
                            data[0], data[1], "customer_support", data[2],
                            branch.getId(), branch.getLocation()
                        );
                        session.save(account);
                        System.out.println("Added customer support: " + data[0] + " for branch: " + branch.getLocation());
                    } else {
                        System.out.println("Customer support with phone " + data[1] + " already exists, skipped.");
                    }
                }
            }

            session.getTransaction().commit();
            System.out.println("Customer support initialization completed");
        } catch (Exception e) {
            System.err.println("Failed to initialize customer support: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void initializeNutritionists() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();
            
            // Get all branches to assign nutritionists
            List<Branch> branches = session.createQuery("from Branch").getResultList();
            
            if (branches.isEmpty()) {
                System.out.println("No branches found for nutritionist assignment");
                session.getTransaction().commit();
                return;
            }
            
            // Nutritionist data per branch (multiple per branch)
            String[][][] nutritionistData = {
                // Tel-Aviv branch nutritionists
                {
                    {"Rachel Green", "0535555001", "rachel123"},
                    {"Jonathan Klein", "0535555002", "jonathan123"}
                },
                // Haifa branch nutritionists
                {
                    {"Tamar Levin", "0536666001", "tamar123"},
                    {"Eitan Paz", "0536666002", "eitan123"}
                },
                // Jerusalem branch nutritionists
                {
                    {"Michal Keren", "0537777001", "michal123"},
                    {"Yossi Berger", "0537777002", "yossi123"}
                },
                // Beer-Sheva branch nutritionists
                {
                    {"Liora Shani", "0538888001", "liora123"},
                    {"Boaz Cohen", "0538888002", "boaz123"}
                }
            };
            
            // Create nutritionist accounts for each branch
            for (int i = 0; i < Math.min(branches.size(), nutritionistData.length); i++) {
                Branch branch = branches.get(i);
                String[][] branchNutritionistData = nutritionistData[i];
                
                for (String[] data : branchNutritionistData) {
                    if (!userExists(session, data[1])) {
                        UserAccount account = new UserAccount(
                            data[0], data[1], "nutritionist", data[2],
                            branch.getId(), branch.getLocation()
                        );
                        session.save(account);
                        System.out.println("Added nutritionist: " + data[0] + " for branch: " + branch.getLocation());
                    } else {
                        System.out.println("Nutritionist with phone " + data[1] + " already exists, skipped.");
                    }
                }
            }

            session.getTransaction().commit();
            System.out.println("Nutritionist initialization completed");
        } catch (Exception e) {
            System.err.println("Failed to initialize nutritionists: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if users with a specific role already exist in the database
     */
    private static boolean roleExists(String role) {
        try (Session session = factory.openSession()) {
            String hql = "SELECT COUNT(u) FROM UserAccount u WHERE u.role = :role";
            Long count = session.createQuery(hql, Long.class)
                .setParameter("role", role)
                .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            System.err.println("Error checking if role exists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Initialize branch-specific menus with special items for each branch
     */
    public static void initializeBranchMenus() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            // Get all branches
            List<Branch> branches = session.createQuery("from Branch", Branch.class).getResultList();
            
            for (Branch branch : branches) {
                // Create special items for each branch
                MenuItem[] branchItems = {
                    new MenuItem("Special " + branch.getLocation() + " Pizza", 
                               "Premium cheese, Fresh basil, Cherry tomatoes", 
                               "House special", 75.00, 
                               "Main course", branch),
                    new MenuItem("House " + branch.getLocation() + " Salad", 
                               "Mixed greens, Nuts, House dressing", 
                               "Vegetarian", 45.00, 
                               "First meal", branch),
                    new MenuItem(branch.getLocation() + " Special Dessert", 
                               "Chef's special creation", 
                               "Daily special", 40.00, 
                               "Desert", branch)
                };

                for (MenuItem item : branchItems) {
                    // Set image paths for branch-specific items
                    setImagePathForMenuItem(item);
                    session.save(item);
                    System.out.println("Added special item: " + item.getName() + " to branch: " + branch.getLocation());
                }
            }

            session.getTransaction().commit();
            System.out.println("Branch menus initialization completed");
        } catch (Exception e) {
            System.err.println("Failed to initialize branch menus: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ensures each branch has at least one special menu item
     * This is a safety method that will run even on an initialized database.
     */
    public static void ensureBranchMenusExist() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();
            
            // Get all branches
            List<Branch> branches = session.createQuery("from Branch", Branch.class).getResultList();
            if (branches.isEmpty()) {
                System.out.println("No branches found for menu item verification");
                session.getTransaction().commit();
                return;
            }
            
            System.out.println("Checking branch menu items for " + branches.size() + " branches...");
            int specialItemsCreated = 0;
            
            // The special items for each branch (one unique item per branch)
            String[][] specialMenuItems = {
                // Tel-Aviv special - format: name, ingredients, preferences, price, category
                {"Tel-Aviv Fusion Plate", "Local fish, Mediterranean herbs, Tahini sauce", "Chef's special", "85.00", "First meal"},
                
                // Haifa special
                {"Haifa Seafood Platter", "Fresh catch of the day, garlic butter, lemon", "Locally sourced", "95.00", "Main course"},
                
                // Jerusalem special
                {"Jerusalem Mixed Grill", "Chicken hearts, livers, and spices", "Traditional recipe", "78.00", "Appetizer"},
                
                // Beer-Sheva special
                {"Negev Desert Lamb", "Slow-cooked lamb, date honey, pine nuts", "Bedouin-inspired", "89.00", "Desert"}
            };
            
            // Check each branch and create special menu item if needed
            for (int i = 0; i < Math.min(branches.size(), specialMenuItems.length); i++) {
                Branch branch = branches.get(i);
                String[] specialItemData = specialMenuItems[i];
                
                // Check if branch already has any special menu items
                List<MenuItem> branchItems = session.createQuery(
                    "FROM MenuItem WHERE branch = :branch", 
                    MenuItem.class
                )
                .setParameter("branch", branch)
                .getResultList();
                
                // If branch has no special items, create one
                if (branchItems.isEmpty()) {
                    System.out.println("Creating special menu item for branch: " + branch.getLocation());
                    
                    // Create the branch's special item with direct branch association
                    MenuItem specialItem = new MenuItem(
                        specialItemData[0],
                        specialItemData[1], 
                        specialItemData[2],
                        Double.parseDouble(specialItemData[3]),
                        specialItemData[4],  // Use the proper category
                        branch
                    );
                    
                    // Set the branch directly to ensure proper association
                    specialItem.setBranch(branch);
                    session.save(specialItem);
                    
                    System.out.println("Created special menu item for branch: " + branch.getLocation() + 
                                    ": " + specialItemData[0]);
                    specialItemsCreated++;
                } else {
                    System.out.println("Branch " + branch.getLocation() + " already has " + 
                                       branchItems.size() + " special menu items");
                }
            }
            
            session.getTransaction().commit();
            System.out.println("Branch menu verification completed. Created " + specialItemsCreated + " new special menu items.");
            
            // After transactions are complete, debug the current menu state
            debugMenuState();
        } catch (Exception e) {
            System.err.println("Error ensuring branch menu items exist: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Debug method to print all menu items and their branch associations
     */
    private static void debugMenuState() {
        try (Session session = factory.openSession()) {
            System.out.println("\n=== DEBUG: MENU ITEM STATE ===");
            
            // Get default menu items (no branch association)
            List<MenuItem> defaultItems = session.createQuery(
                "FROM MenuItem WHERE branch IS NULL", 
                MenuItem.class
            ).getResultList();
            System.out.println("\nDefault menu has " + defaultItems.size() + " items:");
            for (MenuItem item : defaultItems) {
                System.out.println("  - " + item.getName() + " (ID: " + item.getId() + 
                                 ", Category: " + item.getCategory() + ")");
            }
            
            // Find branch-specific menu items
            List<Branch> branches = session.createQuery("FROM Branch", Branch.class).getResultList();
            System.out.println("\nBranch-specific menu items:");
            for (Branch branch : branches) {
                List<MenuItem> branchItems = session.createQuery(
                    "FROM MenuItem WHERE branch = :branch", 
                    MenuItem.class
                )
                .setParameter("branch", branch)
                .getResultList();
                
                System.out.println("Branch: " + branch.getLocation() + " (ID: " + branch.getId() + 
                                  ") has " + branchItems.size() + " special menu items:");
                for (MenuItem item : branchItems) {
                    System.out.println("  - " + item.getName() + " (ID: " + item.getId() + 
                                     ", Category: " + item.getCategory() + ")");
                }
            }
            
            System.out.println("=== END DEBUG ===\n");
        } catch (Exception e) {
            System.err.println("Error in debug menu state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void initializeReservations() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            // Get all branches and tables
            List<Branch> branches = session.createQuery("from Branch", Branch.class).getResultList();
            List<RestaurantTable> tables = session.createQuery("from RestaurantTable", RestaurantTable.class).getResultList();

            System.out.println("Found " + branches.size() + " branches and " + tables.size() + " tables");

            if (!branches.isEmpty() && !tables.isEmpty()) {
                System.out.println("Creating dummy reservations...");

                // Client data
                String[][] clientData = {
                    {"John Doe", "0501234567", "password123"},
                    {"Jane Smith", "0507654321", "password123"},
                    {"Bob Johnson", "0509876543", "password123"}
                };

                // Create or get existing clients
                List<Client> clients = new ArrayList<>();
                
                for (String[] data : clientData) {
                    // Check if user account exists
                    UserAccount existingAccount = null;
                    
                    try {
                        List<UserAccount> accounts = session.createQuery(
                            "FROM UserAccount WHERE phoneNumber = :phone", 
                            UserAccount.class)
                            .setParameter("phone", data[1])
                            .getResultList();
                        
                        if (!accounts.isEmpty()) {
                            existingAccount = accounts.get(0);
                            System.out.println("Found existing account for " + data[0] + " with phone " + data[1]);
                        }
                    } catch (Exception e) {
                        System.err.println("Error checking for existing account: " + e.getMessage());
                    }
                    
                    Client client;
                    
                    if (existingAccount != null) {
                        // Try to find the client associated with this account
                        List<Client> existingClients = session.createQuery(
                            "FROM Client WHERE account.id = :accountId", 
                            Client.class)
                            .setParameter("accountId", existingAccount.getId())
                            .getResultList();
                        
                        if (!existingClients.isEmpty()) {
                            client = existingClients.get(0);
                            System.out.println("Using existing client: " + client.getName());
                        } else {
                            // Create a new client with the existing account
                            client = new Client(data[0], existingAccount);
                            session.save(client);
                            System.out.println("Created new client for existing account: " + data[0]);
                        }
                    } else {
                        // Create a new account and client
                        UserAccount newAccount = new UserAccount(data[0], data[1], "client", data[2]);
                        session.save(newAccount);
                        
                        client = new Client(data[0], newAccount);
                        session.save(client);
                        System.out.println("Created new client with new account: " + data[0]);
                    }
                    
                    clients.add(client);
                }

                // Create reservations for each branch
                for (Branch branch : branches) {
                    System.out.println("Processing branch: " + branch.getLocation());
                    
                    // Get tables for this branch
                    List<RestaurantTable> branchTables = new ArrayList<>();
                    for (RestaurantTable table : tables) {
                        if (table.getLocation().equals(branch.getLocation())) {
                            branchTables.add(table);
                        }
                    }
                    
                    System.out.println("Found " + branchTables.size() + " tables for branch " + branch.getLocation());

                    if (!branchTables.isEmpty()) {
                        // Create 2-3 reservations per branch
                        for (int i = 0; i < 3; i++) {
                            // Create a reservation for today at different times
                            LocalDateTime reservationTime = LocalDateTime.now()
                                .withHour(12 + i * 2)  // 12:00, 14:00, 16:00
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);

                            // Select 1-2 tables for this reservation
                            List<RestaurantTable> selectedTables = new ArrayList<>();
                            selectedTables.add(branchTables.get(i % branchTables.size()));
                            if (branchTables.size() > 1) {
                                selectedTables.add(branchTables.get((i + 1) % branchTables.size()));
                            }

                            // Get the client for this reservation
                            Client reservationClient = clients.get(i % clients.size());

                            // Create the reservation
                            Reservation reservation = new Reservation(
                                reservationTime,
                                2 + i,  // 2, 3, 4 guests
                                reservationClient.getAccount().getPhoneNumber(),
                                branch,
                                selectedTables
                            );

                            // Set the client
                            reservation.setClient(reservationClient);

                            // Save the reservation
                            session.save(reservation);
                            System.out.println("Created reservation for " + branch.getLocation() + 
                                             " at " + reservationTime + 
                                             " with " + selectedTables.size() + " tables" +
                                             " for client " + reservationClient.getName());

                            // Mark tables as reserved
                            for (RestaurantTable table : selectedTables) {
                                int startIndex = reservationTime.getHour() * 60 + reservationTime.getMinute();
                                boolean[] timeSlots = table.getMinutes();
                                // Ensure we don't exceed array bounds
                                int endIndex = Math.min(startIndex + Reservation.DEFAULT_DURATION_MINUTES, timeSlots.length);
                                for (int j = startIndex; j < endIndex; j++) {
                                    timeSlots[j] = true;
                                }
                                table.setMinutes(timeSlots);
                                session.update(table);
                            }
                        }
                    }
                }
            }

            session.getTransaction().commit();
            System.out.println("Reservations initialization completed");
            
            // Verify the reservations were created
            try (Session verifySession = factory.openSession()) {
                List<Reservation> reservations = verifySession.createQuery("from Reservation", Reservation.class).getResultList();
                System.out.println("Total reservations created: " + reservations.size());
                for (Reservation reservation : reservations) {
                    System.out.println("Reservation ID: " + reservation.getId() + 
                                     ", Branch: " + reservation.getBranch().getLocation() +
                                     ", Time: " + reservation.getReservationTime() +
                                     ", Tables: " + reservation.getTables().size() +
                                     ", Client: " + reservation.getClient().getName());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize reservations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeOrders() {
        try (Session session = Database.getSessionFactoryInstance().openSession()) {
            Transaction transaction = session.beginTransaction();
            
            // Check if orders already exist
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
            countQuery.select(builder.count(countQuery.from(Order.class)));
            Long count = session.createQuery(countQuery).getSingleResult();
            
            if (count > 0) {
                System.out.println("Orders already exist in the database. Skipping initialization.");
                transaction.commit();
                return;
            }
            
            System.out.println("Creating test orders...");
            
            // Create sample orders
            LocalDateTime now = LocalDateTime.now();
            
            // Order 1: Regular delivery order
            Order order1 = new Order(1, 150.00, now, null);
            order1.setStatus("Pending");
            order1.setCustomerName("John Doe");
            order1.setPhoneNumber("0544347642");
            order1.setDeliveryDate("2025-04-09");
            order1.setDeliveryTime("12:40");
            order1.setDeliveryLocation("123 Main St, Tel Aviv");
            order1.setPaymentMethod("Credit Card");
            session.save(order1);
            
            // Order 2: Takeout order
            Order order2 = new Order(2, 85.50, now.plusHours(1), null);
            order2.setStatus("Completed");
            order2.setCustomerName("Jane Smith");
            order2.setPhoneNumber("0521234567");
            order2.setDeliveryDate("2025-04-09");
            order2.setDeliveryTime("13:30");
            order2.setDeliveryLocation("Takeout");
            order2.setPaymentMethod("Cash");
            session.save(order2);
            
            // Order 3: Large group order
            Order order3 = new Order(1, 450.75, now.plusHours(2), null);
            order3.setStatus("Pending");
            order3.setCustomerName("Mike Johnson");
            order3.setPhoneNumber("0539876543");
            order3.setDeliveryDate("2025-04-09");
            order3.setDeliveryTime("14:00");
            order3.setDeliveryLocation("456 Park Ave, Jerusalem");
            order3.setPaymentMethod("Credit Card");
            session.save(order3);
            
            transaction.commit();
            System.out.println("Created 3 test orders successfully");
            
            // Verify orders were created
            List<Order> orders = session.createQuery("FROM Order", Order.class).list();
            System.out.println("Total orders in database: " + orders.size());
            
        } catch (Exception e) {
            System.err.println("Error initializing orders: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sets appropriate image path for a menu item based on its name and category
     */
    private static void setImagePathForMenuItem(MenuItem item) {
        String itemName = item.getName().toLowerCase();
        String category = item.getCategory().toLowerCase();

        // IMPORTANT: imagePath should be just the filename. The client prefixes the folder path.
        if (itemName.contains("pizza")) {
            item.setImagePath("pizza.jpg");
        } else if (itemName.equals("hamburger") || (itemName.contains("hamburger") && !itemName.contains("vegan"))) {
            item.setImagePath("burger.jpg");
        } else if (itemName.contains("vegan") && itemName.contains("hamburger")) {
            item.setImagePath("Veggie-Burger.jpg");
        } else if (itemName.contains("pasta")) {
            item.setImagePath("pasta.png");
        } else if (itemName.contains("greek") || itemName.contains("caesar") || itemName.contains("salad")) {
            item.setImagePath("salad.png");
        } else if (itemName.contains("coca") || itemName.contains("cola")) {
            item.setImagePath("cola.jpg");
        } else if (itemName.contains("tea")) {
            item.setImagePath("iced-tea.jpg");
        } else if (itemName.contains("bread")) {
            item.setImagePath("Easy Garlic Bread with Sliced Bread.jpg");
        } else if (itemName.contains("cake") || itemName.contains("chocolate")) {
            item.setImagePath("chocolate cake.jpg");
        } else if (itemName.contains("juice") || itemName.contains("orange")) {
            item.setImagePath("orange juice.jpg");
        } else if (itemName.contains("tiramisu")) {
            item.setImagePath("tiramisu.jpg");
        } else if (category.contains("desert")) {
            // Dessert category default
            item.setImagePath("chocolate cake.jpg");
        } else {
            // Final fallback
            item.setImagePath("default_meal.jpg");
        }
    }
}