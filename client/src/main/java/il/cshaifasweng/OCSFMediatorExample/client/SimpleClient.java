package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Cart;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleClient extends AbstractClient {

	private static SimpleClient client = null;
	private static final Cart cart = new Cart();
	private static List<MenuItem> cachedMenuItems = new ArrayList<>();
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

	public static Cart getCart() {
		return cart;
	}

	public static List<MenuItem> getMenuItems() {
		return cachedMenuItems;
	}

	// ✅ Place order by sending cart data
	public void placeOrder(Cart cart) throws IOException {
		StringBuilder builder = new StringBuilder("PLACE_ORDER_CART;");
		builder.append(cart.getId()).append(";");

		for (MenuItem item : cart.getItems()) {
			builder.append(item.getId()).append(",");
		}

		if (!cart.getItems().isEmpty()) {
			builder.setLength(builder.length() - 1);
		}

		sendToServer(builder.toString());
	}

	@Override
	protected void handleMessageFromServer(Object msg) {
		System.out.println("Received message from server: " + msg.getClass().getSimpleName());

		if (msg instanceof Warning) {
			System.out.println("Received warning: " + ((Warning) msg).getMessage());
		} else if (msg instanceof String) {
			handleServerResponse((String) msg);
		} else if (msg instanceof List) {
			cachedMenuItems = (List<MenuItem>) msg;
			System.out.println("Received " + cachedMenuItems.size() + " menu items from server.");
			if (menuUpdateListener != null) {
				menuUpdateListener.onMenuUpdate(cachedMenuItems);
			}
		} else {
			System.out.println("Unhandled message type: " + msg.getClass());
		}
	}

	private void handleServerResponse(String message) {
		try {
			if (message.startsWith("UPDATE_PRICE_SUCCESS")) {
				System.out.println("✅ Price updated: " + message);
				sendToServer("GET_MENU");

			} else if (message.startsWith("UPDATE_PRICE_FAILURE")) {
				System.out.println("❌ Failed to update price: " + message.split(";")[1]);

			} else if (message.startsWith("ADD_ITEM_SUCCESS")) {
				System.out.println("✅ New item added.");
				sendToServer("GET_MENU");

			} else if (message.startsWith("ADD_ITEM_FAILURE")) {
				System.out.println("❌ Failed to add item: " + message.split(";")[1]);

			} else if (message.startsWith("ORDER_SUCCESS")) {
				System.out.println("✅ Order placed successfully.");

			} else if (message.startsWith("ORDER_FAILURE")) {
				System.out.println("❌ Failed to place order: " + message.split(";")[1]);

			} else if (message.startsWith("DELIVERY_SUCCESS")) {
				System.out.println("✅ Delivery placed successfully.");

			} else if (message.startsWith("DELIVERY_FAILURE")) {
				System.out.println("❌ Failed to place delivery: " + message.split(";")[1]);

			} else {
				System.out.println("ℹ️ Server message: " + message);
			}
		} catch (IOException e) {
			System.err.println("Error handling server response: " + e.getMessage());
		}
	}

	public void setMenuUpdateListener(MenuUpdateListener listener) {
		this.menuUpdateListener = listener;
	}
}
