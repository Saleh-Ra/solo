package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;

import java.io.IOException;
import java.util.List;

public class SimpleClient extends AbstractClient {

	private static SimpleClient client = null;
	private MenuUpdateListener menuUpdateListener;

	private SimpleClient(String host, int port) {
		super(host, port);
		System.out.println("Attempting to connect to server at " + host + ":" + port);
	}

	public static SimpleClient getClient() {
		if (client == null) {
			client = new SimpleClient("localhost", 3000);
		}
		return client;
	}

	@Override
	protected void handleMessageFromServer(Object msg) {
		System.out.println("Received message from server: " + msg.getClass().getSimpleName());

		if (msg instanceof Warning) {
			System.out.println("Received warning: " + ((Warning) msg).getMessage());
		} else if (msg instanceof String) {
			handleServerResponse((String) msg);
		} else if (msg instanceof List) {
			List<MenuItem> menuItems = (List<MenuItem>) msg;
			System.out.println("Received " + menuItems.size() + " menu items from server.");
			if (menuUpdateListener != null) {
				menuUpdateListener.onMenuUpdate(menuItems);
			}
		} else {
			System.out.println("Unhandled message type: " + msg.getClass());
		}
	}

	private void handleServerResponse(String message) {
		if (message.startsWith("UPDATE_PRICE_SUCCESS")) {
			String[] parts = message.split(";");
			String foodName = parts[1];
			String newPrice = parts[2];
			System.out.println("Price updated successfully: " + foodName + " -> " + newPrice);

			// 🔁 Refresh the menu
			try {
				sendToServer("GET_MENU");
			} catch (IOException e) {
				System.err.println("Failed to request menu refresh after update.");
			}

		} else if (message.startsWith("UPDATE_PRICE_FAILURE")) {
			String reason = message.split(";")[1];
			System.out.println("Failed to update price: " + reason);

		} else if (message.startsWith("ADD_ITEM_SUCCESS")) {
			String itemName = message.split(";")[1];
			System.out.println("New item added successfully: " + itemName);

			// 🔁 Refresh the menu
			try {
				sendToServer("GET_MENU");
			} catch (IOException e) {
				System.err.println("Failed to request menu refresh after adding item.");
			}

		} else if (message.startsWith("ADD_ITEM_FAILURE")) {
			String reason = message.split(";")[1];
			System.out.println("Failed to add item: " + reason);

		} else {
			System.out.println("Server message: " + message);
		}
	}

	public void setMenuUpdateListener(MenuUpdateListener listener) {
		this.menuUpdateListener = listener;
	}
}