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

import static il.cshaifasweng.OCSFMediatorExample.server.SimpleServer.SubscribersList;

public class MenuHandler {

    public static void handleAddItemRequest(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        if (parts.length < 7) {
            SimpleServer.sendFailureResponse(client, "ADD_ITEM_FAILURE", "Invalid format");
            return;
        }

        String name = parts[1];
        String ingredients = parts[2];
        String preferences = parts[3];
        String category = parts[4];
        double price;
        try {
            price = Double.parseDouble(parts[5]);
        } catch (NumberFormatException e) {
            SimpleServer.sendFailureResponse(client, "ADD_ITEM_FAILURE", "Invalid price format");
            return;
        }

        // Get the branch ID from the request
        Integer branchId = null;
        try {
            branchId = Integer.parseInt(parts[6]);
        } catch (NumberFormatException e) {
            SimpleServer.sendFailureResponse(client, "ADD_ITEM_FAILURE", "Invalid branch ID");
            return;
        }

        // Add item to branch-specific menu
        boolean success = addItemToBranch(name, ingredients, preferences, price, category, branchId);

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
    public static void sendMenuToClient(String msgString, ConnectionToClient client) {
        try {
            SessionFactory factory = Database.getSessionFactoryInstance();
            Session session = factory.openSession();

            Integer branchId = null;
            if (msgString != null && msgString.contains(";")) {
                try {
                    branchId = Integer.parseInt(msgString.split(";", 2)[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }

            List<MenuItem> menuItems;
            if (branchId == null) {
                System.out.println("Sending ONLY universal menu items (branch_id IS NULL) to client");
                menuItems = session.createNativeQuery(
                    "SELECT * FROM MenuItem WHERE branch_id IS NULL",
                    MenuItem.class
                ).getResultList();
            } else {
                System.out.println("Sending universal items + branch specials for branch ID: " + branchId);
                menuItems = session.createNativeQuery(
                    "SELECT * FROM MenuItem WHERE branch_id IS NULL OR branch_id = :branchId",
                    MenuItem.class
                ).setParameter("branchId", branchId).getResultList();
            }

            System.out.println("Total menu items being sent: " + menuItems.size());
            client.sendToClient(menuItems);
            session.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMenuByCategoryToClient(String msgString, ConnectionToClient client) {
        try {
            String[] parts = msgString.split(";");
            if (parts.length < 2) {
                SimpleServer.sendFailureResponse(client, "GET_MENU_BY_CATEGORY_FAILURE", "Missing category");
                return;
            }
            String category = parts[1];
            Integer branchId = null;
            if (parts.length >= 3) {
                try {
                    branchId = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException ignored) {
                }
            }

            SessionFactory factory = Database.getSessionFactoryInstance();
            Session session = factory.openSession();

            List<MenuItem> items;
            if (branchId == null) {
                // Only universal items of this category
                items = session.createNativeQuery(
                    "SELECT * FROM MenuItem WHERE branch_id IS NULL AND category = :cat",
                    MenuItem.class
                ).setParameter("cat", category).getResultList();
            } else {
                // Universal + this branch's specials for this category
                items = session.createNativeQuery(
                    "SELECT * FROM MenuItem WHERE category = :cat AND (branch_id IS NULL OR branch_id = :branchId)",
                    MenuItem.class
                ).setParameter("cat", category).setParameter("branchId", branchId).getResultList();
            }

            client.sendToClient(items);
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
                System.out.println("  - Default Item: " + item.getName() + 
                                  " (ID: " + item.getId() + 
                                  ", Category: " + item.getCategory() + ")");
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
    private static boolean addItemToBranch(String name, String ingredients, String preferences, double price, String category, Integer branchId) {
        SessionFactory factory = Database.getSessionFactoryInstance();
        try (Session session = factory.openSession()) {
            session.beginTransaction();
            
            // Find the branch by ID
            Branch branch = session.get(Branch.class, branchId);
            if (branch == null) {
                return false;
            }
            
            // Create new menu item and directly associate it with the branch entity
            MenuItem newItem = new MenuItem(name, ingredients, preferences, price, category, branch);
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

    public static void handleDeleteItemRequest(String msgString, ConnectionToClient client) {
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

    public static void handleToggleItemScope(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        if (parts.length < 3) {
            SimpleServer.sendFailureResponse(client, "TOGGLE_ITEM_SCOPE_FAILURE", "Invalid format");
            return;
        }
        String name = parts[1].trim();
        Integer branchId;
        try {
            branchId = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException e) {
            SimpleServer.sendFailureResponse(client, "TOGGLE_ITEM_SCOPE_FAILURE", "Invalid branch id");
            return;
        }

        SessionFactory factory = Database.getSessionFactoryInstance();
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            List<MenuItem> items = session.createNativeQuery(
                "SELECT * FROM MenuItem WHERE name = :name",
                MenuItem.class
            ).setParameter("name", name).getResultList();
            if (items.isEmpty()) {
                session.getTransaction().commit();
                SimpleServer.sendFailureResponse(client, "TOGGLE_ITEM_SCOPE_FAILURE", "Item not found");
                return;
            }

            MenuItem item = items.get(0);
            if (item.getBranch() == null) {
                // Make it special for this branch
                Branch branch = session.get(Branch.class, branchId);
                if (branch == null) {
                    session.getTransaction().commit();
                    SimpleServer.sendFailureResponse(client, "TOGGLE_ITEM_SCOPE_FAILURE", "Branch not found");
                    return;
                }
                item.setBranch(branch);
            } else {
                // Make it universal
                item.setBranch(null);
            }

            session.update(item);
            session.getTransaction().commit();
            SimpleServer.sendSuccessResponse(client, "TOGGLE_ITEM_SCOPE_SUCCESS", name);
            SimpleServer.sendUpdatedMenuToAllClients();
        }
    }

    public static void handleUpdateIngredients(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";", 3);
        if (parts.length < 3) {
            SimpleServer.sendFailureResponse(client, "UPDATE_INGREDIENTS_FAILURE", "Invalid format");
            return;
        }
        String name = parts[1].trim();
        String newIngredients = parts[2].trim();

        int updated = DataManager.updateFieldByCondition(MenuItem.class, "ingredients", newIngredients, "name", name);
        if (updated > 0) {
            SimpleServer.sendSuccessResponse(client, "UPDATE_INGREDIENTS_SUCCESS", name);
            SimpleServer.sendUpdatedMenuToAllClients();
        } else {
            SimpleServer.sendFailureResponse(client, "UPDATE_INGREDIENTS_FAILURE", "Item not found");
        }
    }
    /*public static ConnectionToClient getClientByPhone(String phone) {
        for (SubscribedClient sc : SubscribersList) {
            if (sc.getClient() != null && sc.getPhoneNumber().equals(phone)) {
                return sc.getClient();
            }
        }
        return null; // Not connected
    }*/
}
