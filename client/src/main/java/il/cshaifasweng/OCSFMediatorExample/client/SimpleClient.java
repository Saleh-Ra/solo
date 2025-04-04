package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Cart;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	@Override
	protected void handleMessageFromServer(Object msg) {
		System.out.println("Received message from server: " + msg);
		// Check if it's a Warning
		if (msg instanceof Warning) {
			Warning warning = (Warning) msg;
			String message = warning.getMessage();
			System.out.println("Handling warning: " + message);
			
			// Handle login responses
			if (message.startsWith("LOGIN_SUCCESS")) {
				try {
					App.setRoot("personal_area");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} 
			// Handle signup responses
			else if (message.startsWith("SIGNUP_SUCCESS")) {
				try {
					System.out.println("Signup successful, redirecting to sign-in page");
					App.setRoot("sign_in");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// Post other warnings to EventBus for display
			else {
				EventBus.getDefault().post(new WarningEvent(warning));
			}
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

	public void setMenuUpdateListener(MenuUpdateListener listener) {
		this.menuUpdateListener = listener;
	}

	public void placeOrder(Cart cart) throws IOException {
		StringBuilder builder = new StringBuilder("PLACE_ORDER_CART;");
		builder.append(cart.getId()).append(";");

		for (Map.Entry<MenuItem, Integer> entry : cart.getItems().entrySet()) {
			MenuItem item = entry.getKey();
			int quantity = entry.getValue();
			for (int i = 0; i < quantity; i++) {
				builder.append(item.getId()).append(",");
			}
		}

		if (!cart.getItems().isEmpty()) {
			builder.setLength(builder.length() - 1);
		}

		sendToServer(builder.toString());
	}

	@Override
	public void sendToServer(Object msg) throws IOException {
		System.out.println("Sending to server: " + msg);
		super.sendToServer(msg);
	}
}
