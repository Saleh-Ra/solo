package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Cart;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.Reservation;
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
	private static String currentUserPhone;
	private static String currentUserRole;
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
	
	public static String getCurrentUserPhone() {
		return currentUserPhone;
	}
	
	public static void setCurrentUserPhone(String phoneNumber) {
		currentUserPhone = phoneNumber;
		System.out.println("Set current user phone to: " + phoneNumber);
	}

	public static String getCurrentUserRole() {
		return currentUserRole;
	}

	public static void setCurrentUserRole(String role) {
		currentUserRole = role;
		System.out.println("Set current user role to: " + role);
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
					// Extract phone number and role from login success message
					String[] parts = message.split(";");
					if (parts.length > 2) {
						String phoneNumber = parts[1];
						String role = parts[2];
						setCurrentUserPhone(phoneNumber);
						setCurrentUserRole(role);
						System.out.println("Logged in user with phone: " + phoneNumber + " and role: " + role);
						
						// Redirect based on role
						if ("manager".equalsIgnoreCase(role)) {
							App.setRoot("secondary2");
						} else {
							App.setRoot("personal_area");
						}
					}
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
			// Check if it's a list of orders
			if (!((List<?>) msg).isEmpty() && ((List<?>) msg).get(0) instanceof Order) {
				List<Order> orders = (List<Order>) msg;
				System.out.println("Received " + orders.size() + " orders from server");
				EventBus.getDefault().post(new OrdersReceivedEvent(orders));
			}
			// Check if it's a list of reservations
			else if (!((List<?>) msg).isEmpty() && ((List<?>) msg).get(0) instanceof Reservation) {
				List<Reservation> reservations = (List<Reservation>) msg;
				System.out.println("Received " + reservations.size() + " reservations from server");
				EventBus.getDefault().post(new ReservationsReceivedEvent(reservations));
			}
			// Or it's an empty list - check the instance type from generic signature
			else if (((List<?>) msg).isEmpty()) {
				// Since we can't tell what type of empty list it is, post both empty events
				// The controller will handle whichever is relevant
				System.out.println("Received empty list from server");
				EventBus.getDefault().post(new OrdersReceivedEvent(new ArrayList<>()));
				EventBus.getDefault().post(new ReservationsReceivedEvent(new ArrayList<>()));
			}
			// Otherwise assume it's menu items
			else {
				cachedMenuItems = (List<MenuItem>) msg;
				System.out.println("Received " + cachedMenuItems.size() + " menu items from server.");
				if (menuUpdateListener != null) {
					menuUpdateListener.onMenuUpdate(cachedMenuItems);
				}
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
	
	public void getUserOrders() throws IOException {
		if (currentUserPhone != null && !currentUserPhone.isEmpty()) {
			String message = "GET_USER_ORDERS;" + currentUserPhone;
			sendToServer(message);
			System.out.println("Requesting orders for user with phone: " + currentUserPhone);
		} else {
			System.out.println("Cannot get orders - no current user phone number");
		}
	}

	public void getUserReservations() throws IOException {
		if (currentUserPhone != null && !currentUserPhone.isEmpty()) {
			String message = "GET_USER_RESERVATIONS;" + currentUserPhone;
			sendToServer(message);
			System.out.println("Requesting reservations for user with phone: " + currentUserPhone);
		} else {
			System.out.println("Cannot get reservations - no current user phone number");
		}
	}

	@Override
	public void sendToServer(Object msg) throws IOException {
		System.out.println("Sending to server: " + msg);
		super.sendToServer(msg);
	}
	
	// Event class for orders received
	public static class OrdersReceivedEvent {
		private final List<Order> orders;
		
		public OrdersReceivedEvent(List<Order> orders) {
			this.orders = orders;
		}
		
		public List<Order> getOrders() {
			return orders;
		}
	}

	// Event class for reservations received
	public static class ReservationsReceivedEvent {
		private final List<Reservation> reservations;
		
		public ReservationsReceivedEvent(List<Reservation> reservations) {
			this.reservations = reservations;
		}
		
		public List<Reservation> getReservations() {
			return reservations;
		}
	}
}
