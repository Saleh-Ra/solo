package il.cshaifasweng.OCSFMediatorExample.server.Handlers;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.SimpleServer;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.*;

import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.*;

import java.io.IOException;
import java.util.List;

public class MenuHandler {

    public static void handleAddItemRequest(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        if (parts.length < 5) {
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

        MenuItem newItem = new MenuItem(name, ingredients, preferences, price);
        DataManager.add(newItem);
        System.out.println("Added new item to database: " + name);

        SimpleServer.sendSuccessResponse(client, "ADD_ITEM_SUCCESS", name);
        SimpleServer.sendUpdatedMenuToAllClients();
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

    public static void sendMenuToClient(ConnectionToClient client) {
        try {
            //List<MenuItem> updatedItems = database.getMenuItems(); // Fetch fresh data from DB
            List<MenuItem> updatedItems = DataManager.fetchAll(MenuItem.class); // Fetch fresh data from DB
            System.out.println("Sending menu items to client: " + updatedItems.size() + " items");
            client.sendToClient(updatedItems);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
