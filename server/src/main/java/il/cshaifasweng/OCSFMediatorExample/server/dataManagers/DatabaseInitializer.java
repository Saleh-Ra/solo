package il.cshaifasweng.OCSFMediatorExample.server.dataManagers;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;


public class DatabaseInitializer {

    public static final SessionFactory factory = Database.getSessionFactoryInstance();
    public static void initializeAll() {
        initializeMenu();
        initializeBranches();
        initializeRestaurantChain();
        initializeRestaurantTable();
        initializeManager();
        initializeBranchesManagers();

    }


    public static void initializeMenu() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<MenuItem> query = builder.createQuery(MenuItem.class);
            query.from(MenuItem.class);
            List<MenuItem> existingItems = session.createQuery(query).getResultList();

            if (existingItems.isEmpty()) {
                session.save(new MenuItem("Pizza", "Cheese, Tomato, Onions, Mushroom", "Vegetarian", 50.00));
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

    public static void initializeBranches() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Branch> query = builder.createQuery(Branch.class);
            query.from(Branch.class);
            List<Branch> existingItems = session.createQuery(query).getResultList();

            if (existingItems.isEmpty()) {
                // Branch(String location, String openingHours,List<RestaurantTable> tables)
                RestaurantChain chain=DataManager.fetchByField(RestaurantChain.class,"name","foreign restaurant").get(0);
                session.save(new Branch("Tel-Aviv", "daily 10:00-22:00",chain, DataManager.fetchByIdRange(RestaurantTable.class, 1, 12)));
                session.save(new Branch("Haifa", "from Sunday to Thursday 10:00-22:00",chain, DataManager.fetchByIdRange(RestaurantTable.class, 13, 18)));
                session.save(new Branch("Jerusalem", "daily 10:00-22:00", chain,DataManager.fetchByIdRange(RestaurantTable.class, 19, 24)));
                session.save(new Branch("Beer-Shiva", "daily 10:00-22:00", chain,DataManager.fetchByIdRange(RestaurantTable.class, 25, 33)));
            }

            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Failed to initialize the branches: " + e.getMessage());
        }
    }

    public static void initializeRestaurantChain() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<RestaurantChain> query = builder.createQuery(RestaurantChain.class);
            query.from(RestaurantChain.class);
            List<RestaurantChain> existingItems = session.createQuery(query).getResultList();

            if (existingItems.isEmpty()) {
                session.save(new RestaurantChain("foreign restaurant",DataManager.fetchByIdRange(Branch.class, 1, 4)));
            }

            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Failed to initialize the chain: " + e.getMessage());
        }
    }

    public static void initializeRestaurantTable() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<RestaurantTable> query = builder.createQuery(RestaurantTable.class);
            query.from(RestaurantTable.class);
            List<RestaurantTable> existingItems = session.createQuery(query).getResultList();

            if (existingItems.isEmpty()) {
                // (int seatingCapacity, int reservedID,String location, String preferences,boolean[] array of 0's)
                session.save(new RestaurantTable(3, 0, "Main", "near bar", "Table 1", new boolean[720]));
                session.save(new RestaurantTable(2, 0, "Main", "center", "Table 2", new boolean[720]));
                session.save(new RestaurantTable(3, 0, "Main", "center", "Table 3", new boolean[720]));
                session.save(new RestaurantTable(4, 0, "Main", "near bar", "Table 4", new boolean[720]));
                session.save(new RestaurantTable(3, 0, "Main", "center", "Table 5", new boolean[720]));
                session.save(new RestaurantTable(2, 0, "Main", "near kitchen", "Table 6", new boolean[720]));
                session.save(new RestaurantTable(3, 0, "Main", "near bar", "Table 7", new boolean[720]));
                session.save(new RestaurantTable(2, 0, "Main", "near kitchen", "Table 8", new boolean[720]));
                session.save(new RestaurantTable(3, 0, "Main", "near bar", "Table 9", new boolean[720]));
                session.save(new RestaurantTable(3, 0, "Main", "center", "Table 10", new boolean[720]));
                session.save(new RestaurantTable(3, 0, "Main", "center", "Table 11", new boolean[720]));
                session.save(new RestaurantTable(4, 0, "Main", "near kitchen", "Table 12", new boolean[720]));

                session.save(new RestaurantTable(4, 0, "Bar", "near kitchen", "Table 13", new boolean[720]));
                session.save(new RestaurantTable(3, 0, "Bar", "quiet", "Table 14", new boolean[720]));
                session.save(new RestaurantTable(2, 0, "Bar", "window", "Table 15", new boolean[720]));
                session.save(new RestaurantTable(2, 0, "Bar", "center", "Table 16", new boolean[720]));
                session.save(new RestaurantTable(3, 0, "Bar", "quiet", "Table 17", new boolean[720]));
                session.save(new RestaurantTable(4, 0, "Bar", "center", "Table 18", new boolean[720]));

                session.save(new RestaurantTable(2, 0, "Out", "cozy", "Table 19", new boolean[720]));
                session.save(new RestaurantTable(4, 0, "Out", "window", "Table 20", new boolean[720]));
                session.save(new RestaurantTable(3, 0, "Out", "quiet", "Table 21", new boolean[720]));
                session.save(new RestaurantTable(2, 0, "Out", "cozy", "Table 22", new boolean[720]));
                session.save(new RestaurantTable(4, 0, "Out", "quiet", "Table 23", new boolean[720]));
                session.save(new RestaurantTable(2, 0, "Out", "window", "Table 24", new boolean[720]));

                session.save(new RestaurantTable(3, 0, "Inside", "center", "Table 25", new boolean[720]));
                session.save(new RestaurantTable(4, 0, "Inside", "quiet", "Table 26", new boolean[720]));
                session.save(new RestaurantTable(2, 0, "Inside", "window", "Table 27", new boolean[720]));
                session.save(new RestaurantTable(3, 0, "Inside", "near kitchen", "Table 28", new boolean[720]));
                session.save(new RestaurantTable(4, 0, "Inside", "quiet", "Table 29", new boolean[720]));
                session.save(new RestaurantTable(2, 0, "Inside", "center", "Table 30", new boolean[720]));
                session.save(new RestaurantTable(2, 0, "Out","window","Table 1",new boolean[720]));
                session.save(new RestaurantTable(3 ,0 ,"Bar" ,"","Table 2",new boolean[720]));
                session.save(new RestaurantTable(4 ,0, "Main","center","Table 3",new boolean[720]));
            }

            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Failed to initialize the menu: " + e.getMessage());
        }
    }

    public static void initializeManager() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<MenuItem> query = builder.createQuery(MenuItem.class);
            query.from(MenuItem.class);
            List<MenuItem> existingItems = session.createQuery(query).getResultList();

            if (existingItems.isEmpty()) {
                // to be chaneged
                session.save(new MenuItem("Pizza", "Cheese, Tomato, Onions, Mushroom", "Vegetarian", 500.00));

            }

            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Failed to initialize the menu: " + e.getMessage());
        }
    }

    public static void initializeChainManager() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<RestaurantChainManager> query = builder.createQuery(RestaurantChainManager.class);
            query.from(RestaurantChainManager.class);
            List<RestaurantChainManager> existingItems = session.createQuery(query).getResultList();

            if (existingItems.isEmpty()) {
                session.save(new RestaurantChainManager("Saleh",new UserAccount("Saleh","0528189099",true,"saleh123")));
            }

            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Failed to initialize the menu: " + e.getMessage());
        }
    }

    public static void initializeBranchesManagers() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<BranchManager> query = builder.createQuery(BranchManager.class);
            query.from(BranchManager.class);
            List<BranchManager> existingItems = session.createQuery(query).getResultList();

            if (existingItems.isEmpty()) {
                // to be chaneged
                Branch branch1=DataManager.fetchByField(Branch.class,"location","Tel-Aviv").get(0);
                UserAccount account1=new UserAccount("Nassim","0544347642",true,"Nassim123");
                session.save(account1);
                session.save(new BranchManager("Nassim",branch1,account1));

                Branch branch2=DataManager.fetchByField(Branch.class,"location","Haifa").get(0);
                UserAccount account2=new UserAccount("Hali","0526112238",true,"Hali123");
                session.save(account2);
                session.save(new BranchManager("Hali",branch2,account2));

                Branch branch3=DataManager.fetchByField(Branch.class,"location","Jerusalem").get(0);
                UserAccount account3=new UserAccount("Mohammad","0543518310",true,"Mohammad123");
                session.save(account3);
                session.save(new BranchManager("Mohammad",branch3,account3));

                Branch branch4=DataManager.fetchByField(Branch.class,"location","Beer-Shiva").get(0);
                UserAccount account4=new UserAccount("Natali","0502404146",true,"Natali123");
                session.save(account4);
                session.save(new BranchManager("Natali",branch4,account4));
            }

            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Failed to initialize the menu: " + e.getMessage());
        }
    }
}
