package il.cshaifasweng.OCSFMediatorExample.server.Handlers;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.SimpleServer;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.*;

import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Menu;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.*;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MenuHandler {

    public static void handleAddItemRequest(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        if (parts.length < 6) {
            SimpleServer.sendFailureResponse(client, "ADD_ITEM_FAILURE", "Invalid format");
            return;
        }

        String name = parts[1];
        String ingredients = parts[2];
        String preferences = parts[3];
        double price;
        try {
            price = Double.parseDouble(parts[4]);
        } catch (NumberFormatException e) {
            SimpleServer.sendFailureResponse(client, "ADD_ITEM_FAILURE", "Invalid price format");
            return;
        }
        
        // Get the branch ID from the request
        Integer branchId = null;
        try {
            branchId = Integer.parseInt(parts[5]);
        } catch (NumberFormatException e) {
            SimpleServer.sendFailureResponse(client, "ADD_ITEM_FAILURE", "Invalid branch ID");
            return;
        }
        
        // Add item to branch-specific menu
        boolean success = addItemToBranch(name, ingredients, preferences, price, branchId);
        
        if (success) {
            System.out.println("Added new item to branch: " + name + " for branch ID: " + branchId);
            SimpleServer.sendSuccessResponse(client, "ADD_ITEM_SUCCESS", name);
            SimpleServer.sendUpdatedMenuToAllClients();
        } else {
            SimpleServer.sendFailureResponse(client, "ADD_ITEM_FAILURE", "Could not find branch");
        }
    }

    /**
     * Sends menu items to client, combining default menu with branch-specific items
     */
    public static void sendMenuToClient(ConnectionToClient client) {
        try {
            SessionFactory factory = Database.getSessionFactoryInstance();
            Session session = factory.openSession();
            List<MenuItem> menuItems = new ArrayList<>();
            
            Object clientInfo = client.getInfo("user");
            System.out.println("Client info: " + (clientInfo == null ? "NULL" : clientInfo.getClass().getName()));
            
            if (clientInfo != null && clientInfo instanceof UserAccount) {
                UserAccount user = (UserAccount) clientInfo;
                System.out.println("User info: name=" + user.getName() + 
                                  ", role=" + user.getRole() + 
                                  ", branchId=" + user.getBranchId() + 
                                  ", branchName=" + user.getBranchName());
                
                if (user.getRole().equals("chain_manager")) {
                    // Chain managers see everything
                    menuItems = session.createNativeQuery("SELECT * FROM MenuItem", MenuItem.class).getResultList();
                    System.out.println("Sending all menu items to chain manager");
                    
                } else if (user.getBranchId() != null) {
                    // Branch managers/staff see default items + their branch's items
                    System.out.println("Executing SQL for branch ID: " + user.getBranchId());
                    menuItems = session.createNativeQuery(
                        "SELECT * FROM MenuItem WHERE branch_id IS NULL OR branch_id = :branchId", 
                        MenuItem.class)
                        .setParameter("branchId", user.getBranchId())
                        .getResultList();
                    System.out.println("SQL executed successfully");
                    System.out.println("Sending default + branch-specific items for branch ID: " + user.getBranchId());
                    
                } else {
                    // Regular users just see default items
                    menuItems = session.createNativeQuery(
                        "SELECT * FROM MenuItem WHERE branch_id IS NULL", 
                        MenuItem.class)
                        .getResultList();
                    System.out.println("Sending default menu to user (no branch ID)");
                }
            } else {
                // Try to find the user account from the database based on current connection
                System.out.println("No user info in client, attempting to retrieve from database...");
                try {
                    // Get connected clients by IP - this is a guess at how you might identify them
                    String clientAddress = client.getInetAddress().getHostAddress();
                    System.out.println("Client address: " + clientAddress);
                    
                    // Try using any other login information you might have stored about this client
                    // For demonstration - use the default menu for now
                    menuItems = session.createNativeQuery(
                        "SELECT * FROM MenuItem WHERE branch_id IS NULL", 
                        MenuItem.class)
                        .getResultList();
                    System.out.println("Sending default menu to client (user info missing)");
                } catch (Exception e) {
                    System.err.println("Error trying to identify client: " + e.getMessage());
                    // Default to just showing default menu
                    menuItems = session.createNativeQuery(
                        "SELECT * FROM MenuItem WHERE branch_id IS NULL", 
                        MenuItem.class)
                        .getResultList();
                    System.out.println("Sending default menu as fallback");
                }
            }
            
            System.out.println("Total menu items being sent: " + menuItems.size());
            client.sendToClient(menuItems);
            session.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Gets ONLY the default menu items (not associated with any branch)
     */
    private static List<MenuItem> getDefaultMenu() {
        SessionFactory factory = Database.getSessionFactoryInstance();
        try (Session session = factory.openSession()) {
            // Use native SQL to bypass any Hibernate entity mapping issues
            List<MenuItem> items = session.createNativeQuery(
                "SELECT * FROM MenuItem WHERE branch_id IS NULL", 
                MenuItem.class
            ).getResultList();
            
            System.out.println("Fetched " + items.size() + " default menu items using direct SQL");
            for (MenuItem item : items) {
                System.out.println("  - Default Item: " + item.getName() + " (ID: " + item.getId() + ")");
            }
            return items;
        } catch (Exception e) {
            System.err.println("Error fetching default menu: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets ONLY the branch-specific items for a particular branch
     */
    private static List<MenuItem> getBranchSpecificItems(Integer branchId) {
        if (branchId == null) {
            return new ArrayList<>();
        }
        
        SessionFactory factory = Database.getSessionFactoryInstance();
        try (Session session = factory.openSession()) {
            // Use native SQL to bypass any Hibernate entity mapping issues
            List<MenuItem> items = session.createNativeQuery(
                "SELECT * FROM MenuItem WHERE branch_id = :branchId", 
                MenuItem.class
            )
            .setParameter("branchId", branchId)
            .getResultList();
            
            System.out.println("Fetched " + items.size() + " items specific to branch ID " + branchId + " using direct SQL");
            for (MenuItem item : items) {
                System.out.println("  - Branch Item: " + item.getName() + " (ID: " + item.getId() + ")");
            }
            return items;
        } catch (Exception e) {
            System.err.println("Error fetching branch items: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets ONLY the branch-specific items from all branches (for chain managers)
     */
    private static List<MenuItem> getAllBranchItems() {
        SessionFactory factory = Database.getSessionFactoryInstance();
        try (Session session = factory.openSession()) {
            // Use native SQL to bypass any Hibernate entity mapping issues
            List<MenuItem> items = session.createNativeQuery(
                "SELECT * FROM MenuItem WHERE branch_id IS NOT NULL", 
                MenuItem.class
            ).getResultList();
            
            System.out.println("Fetched " + items.size() + " branch-specific items across all branches using direct SQL");
            for (MenuItem item : items) {
                System.out.println("  - Branch Item: " + item.getName() + " (ID: " + item.getId() + ")");
            }
            return items;
        } catch (Exception e) {
            System.err.println("Error fetching all branch items: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Add a new menu item to a specific branch
     */
    private static boolean addItemToBranch(String name, String ingredients, String preferences, double price, Integer branchId) {
        SessionFactory factory = Database.getSessionFactoryInstance();
        try (Session session = factory.openSession()) {
            session.beginTransaction();
            
            // Find the branch by ID
            Branch branch = session.get(Branch.class, branchId);
            if (branch == null) {
                return false;
            }
            
            // Create new menu item and directly associate it with the branch entity
            MenuItem newItem = new MenuItem(name, ingredients, preferences, price);
            newItem.setBranch(branch);
            session.save(newItem);
            
            session.getTransaction().commit();
            System.out.println("Successfully added item '" + name + "' to branch ID " + branchId);
            return true;
        } catch (Exception e) {
            System.err.println("Error adding item to branch: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    protected static void handleDeleteItemRequest(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        if (parts.length < 2) {
            SimpleServer.sendFailureResponse(client, "DELETE_ITEM_FAILURE", "Invalid format");
            return;
        }

        String name = parts[1].trim(); // no need to parse or catch NumberFormatException

        List<MenuItem> items = DataManager.fetchByField(MenuItem.class, "name", name);
        if (items.isEmpty()) {
            SimpleServer.sendFailureResponse(client, "DELETE_ITEM_FAILURE", "Item not found");
            return;
        }

        // If you're sure there's only one item with that name, delete the first match
        DataManager.delete(items.get(0));

        SimpleServer.sendSuccessResponse(client, "DELETE_ITEM_SUCCESS", name);
        SimpleServer.sendUpdatedMenuToAllClients();
    }

    public static void handleUpdatePriceRequest(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        if (parts.length < 3) {
            SimpleServer.sendFailureResponse(client, "UPDATE_PRICE_FAILURE", "Invalid format");
            return;
        }

        String mealName = parts[1];
        double newPrice;
        try {
            newPrice = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            SimpleServer.sendFailureResponse(client, "UPDATE_PRICE_FAILURE", "Invalid price format");
            return;
        }

        int updated = DataManager.updateFieldByCondition(MenuItem.class, "price", newPrice, "name", mealName);
        if (updated > 0) {
            SimpleServer.sendSuccessResponse(client, "UPDATE_PRICE_SUCCESS", mealName + ";" + newPrice);
            SimpleServer.sendUpdatedMenuToAllClients(); // Notify all clients about the updated menu
        } else {
            SimpleServer.sendFailureResponse(client, "UPDATE_PRICE_FAILURE", "Item not found");
        }
    }
}
