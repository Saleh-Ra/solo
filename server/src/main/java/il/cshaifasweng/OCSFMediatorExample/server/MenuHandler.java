package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.*;

import java.io.IOException;
import java.util.List;

public class MenuHandler {

    protected static void handleAddItemRequest(String msgString, ConnectionToClient client) {
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

    protected static void handleUpdatePriceRequest(String msgString, ConnectionToClient client) {
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

        boolean success = updateMenuItem(mealName, newPrice);
        if (success) {
            SimpleServer.sendSuccessResponse(client, "UPDATE_PRICE_SUCCESS", mealName + ";" + newPrice);
            SimpleServer.sendUpdatedMenuToAllClients(); // Notify all clients about the updated menu
        } else {
            SimpleServer.sendFailureResponse(client, "UPDATE_PRICE_FAILURE", "Item not found");
        }
    }

    protected static void sendMenuToClient(ConnectionToClient client) {
        try {
            //List<MenuItem> updatedItems = database.getMenuItems(); // Fetch fresh data from DB
            List<MenuItem> updatedItems = DataManager.fetchAll(MenuItem.class); // Fetch fresh data from DB
            System.out.println("Sending menu items to client: " + updatedItems.size() + " items");
            client.sendToClient(updatedItems);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean updateMenuItem(String name, double newPrice) {
        List<MenuItem> items = DataManager.fetchAll(MenuItem.class);
        for (MenuItem item : items) {
            if (item.getName().equals(name)) {
                item.setPrice(newPrice);

                MenuManager.updatePriceByName(name, newPrice);
                System.out.println("Updated price for item: " + name + " to " + newPrice);
                return true;
            }
        }
        return false;
    }
}
