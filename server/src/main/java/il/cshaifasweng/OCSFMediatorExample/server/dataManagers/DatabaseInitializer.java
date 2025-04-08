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
            
            if (!databaseInitialized) {
                System.out.println("Database is empty. Starting fresh initialization...");
                
                // Initialize in the correct order to avoid circular dependencies
                initializeRestaurantTable();  // First tables
                System.out.println("\n✓ Restaurant tables initialized\n");
                
                initializeMenu();             // Then menu items (global default menu)
                System.out.println("\n✓ Default menu initialized\n");
                
                initializeRestaurantChain();  // Then restaurant chain
                System.out.println("\n✓ Restaurant chain initialized\n");
                
                initializeBranches();         // Then branches (which need chain and tables)
                System.out.println("\n✓ Branches initialized\n");
                
                initializeBranchMenus();      // Then branch-specific menus
                System.out.println("\n✓ Branch menus initialized\n");
                
                initializeClientsAndAccounts();  // Then clients and their accounts
                System.out.println("\n✓ Clients initialized\n");
                
                initializeManagers();            // Then branch managers
                System.out.println("\n✓ Managers initialized\n");
            } else {
                System.out.println("Database already contains basic data. Checking for new roles and branch menus...");
                
                // Force check branch menus - this will ensure menus are created even for existing database
                ensureBranchMenusExist();
            }
            
            // Always initialize new roles regardless of whether database is initialized
            // Check if we have any chain managers
            if (!roleExists("chain_manager")) {
                initializeChainManagers();       // Chain managers (new role)
                System.out.println("\n✓ Chain managers initialized\n");
            } else {
                System.out.println("\n✓ Chain managers already exist\n");
            }
            
            // Check if we have any customer support
            if (!roleExists("customer_support")) {
                initializeCustomerSupport();     // Customer support staff (new role)
                System.out.println("\n✓ Customer support initialized\n");
            } else {
                System.out.println("\n✓ Customer support already exists\n");
            }
            
            // Check if we have any nutritionists
            if (!roleExists("nutritionist")) {
                initializeNutritionists();       // Nutritionists (new role)
                System.out.println("\n✓ Nutritionists initialized\n");
            } else {
                System.out.println("\n✓ Nutritionists already exist\n");
            }
            
            System.out.println("\n=== Database initialization completed successfully! ===\n");
            
            // Verify final state
            try (Session session = factory.openSession()) {
                long menuItems = session.createQuery("select count(*) from MenuItem", Long.class).getSingleResult();
                long users = session.createQuery("select count(*) from UserAccount", Long.class).getSingleResult();
                long menus = session.createQuery("select count(*) from Menu", Long.class).getSingleResult();
                System.out.println("Final database state - Menu Items: " + menuItems + ", Users: " + users + ", Menus: " + menus);
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
            
            // First add the chain manager if not exists - chain manager doesn't have branch
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
                    Branch branch = branches.get(i);
                    
                    if (!userExists(session, managerData[idx][1])) {
                        // Use the constructor with branch ID and name
                        UserAccount account = new UserAccount(
                            managerData[idx][0],
                            managerData[idx][1],
                            "manager",
                            managerData[idx][2],
                            branch.getId(),
                            branch.getLocation()
                        );
                        session.save(account);
                        session.save(new BranchManager(managerData[idx][0], branch, account));
                        System.out.println("Added branch manager: " + managerData[idx][0] + " for branch: " + branch.getLocation());
                    } else {
                        System.out.println("Branch manager with phone " + managerData[idx][1] + " already exists, updating branch info if needed.");
                        // Update existing manager with branch info
                        List<UserAccount> existingAccounts = DataManager.fetchUserAccountsByPhoneNumber(managerData[idx][1]);
                        if (!existingAccounts.isEmpty()) {
                            UserAccount existingAccount = existingAccounts.get(0);
                            if (existingAccount.getBranchId() == null || existingAccount.getBranchName() == null) {
                                existingAccount.setBranchId(branch.getId());
                                existingAccount.setBranchName(branch.getLocation());
                                session.update(existingAccount);
                                System.out.println("Updated branch info for manager: " + existingAccount.getName());
                            }
                        }
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
}