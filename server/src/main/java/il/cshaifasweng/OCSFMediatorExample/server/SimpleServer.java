package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleServer extends AbstractServer {
	private static final ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();

	public SimpleServer(int port) {
		super(port);
		DatabaseInitializer.initializeMenu(); // Initialize the database with default menu items
	}

	public static void main(String[] args) {
		SimpleServer server = new SimpleServer(3000);
		try {
			server.listen();
			System.out.println("Server started and listening on port 3000");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		String msgString = msg.toString();
		System.out.println("Received message from client: " + msgString);

		if (msgString.startsWith("GET_MENU")) {
			sendMenuToClient(client);
		} else if (msgString.startsWith("UPDATE_PRICE")) {
			handleUpdatePriceRequest(msgString, client);
		} else if (msgString.startsWith("ADD_ITEM")) {
			handleAddItemRequest(msgString, client);
		}
	}

	private void sendMenuToClient(ConnectionToClient client) {
		try {
			//List<MenuItem> updatedItems = database.getMenuItems(); // Fetch fresh data from DB
			List<MenuItem> updatedItems = DataManager.fetchAll(MenuItem.class); // Fetch fresh data from DB
			System.out.println("Sending menu items to client: " + updatedItems.size() + " items");
			client.sendToClient(updatedItems);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleUpdatePriceRequest(String msgString, ConnectionToClient client) {
		String[] parts = msgString.split(";");
		if (parts.length < 3) {
			sendFailureResponse(client, "UPDATE_PRICE_FAILURE", "Invalid format");
			return;
		}

		String mealName = parts[1];
		double newPrice;
		try {
			newPrice = Double.parseDouble(parts[2]);
		} catch (NumberFormatException e) {
			sendFailureResponse(client, "UPDATE_PRICE_FAILURE", "Invalid price format");
			return;
		}

		boolean success = updateMenuItem(mealName, newPrice);
		if (success) {
			sendSuccessResponse(client, "UPDATE_PRICE_SUCCESS", mealName + ";" + newPrice);
			sendUpdatedMenuToAllClients(); // Notify all clients about the updated menu
		} else {
			sendFailureResponse(client, "UPDATE_PRICE_FAILURE", "Item not found");
		}
	}

	private void handleAddItemRequest(String msgString, ConnectionToClient client) {
		String[] parts = msgString.split(";");
		if (parts.length < 5) {
			sendFailureResponse(client, "ADD_ITEM_FAILURE", "Invalid format");
			return;
		}

		String name = parts[1];
		String ingredients = parts[2];
		String preferences = parts[3];
		double price;
		try {
			price = Double.parseDouble(parts[4]);
		} catch (NumberFormatException e) {
			sendFailureResponse(client, "ADD_ITEM_FAILURE", "Invalid price format");
			return;
		}

		MenuItem newItem = new MenuItem(name, ingredients, preferences, price);
		DataManager.add(newItem);
		System.out.println("Added new item to database: " + name);

		sendSuccessResponse(client, "ADD_ITEM_SUCCESS", name);
		sendUpdatedMenuToAllClients();
	}

	private boolean updateMenuItem(String name, double newPrice) {
		List<MenuItem> items = DataManager.fetchAll(MenuItem.class);
		for (MenuItem item : items) {
			if (item.getName().equals(name)) {
				item.setPrice(newPrice);
				//database.updatePriceByName(name, newPrice);
				System.out.println("Updated price for item: " + name + " to " + newPrice);
				return true;
			}
		}
		return false;
	}

	private void sendUpdatedMenuToAllClients() {
		List<MenuItem> updatedItems = DataManager.fetchAll(MenuItem.class);
		try {
			for (SubscribedClient subscribedClient : SubscribersList) {
				subscribedClient.getClient().sendToClient(updatedItems);
			}
			System.out.println("Sent updated menu to all clients.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendSuccessResponse(ConnectionToClient client, String responseType, String message) {
		try {
			client.sendToClient(responseType + ";" + message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendFailureResponse(ConnectionToClient client, String responseType, String reason) {
		try {
			client.sendToClient(responseType + ";" + reason);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
