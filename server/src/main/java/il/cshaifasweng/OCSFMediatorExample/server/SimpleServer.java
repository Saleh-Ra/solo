package il.cshaifasweng.OCSFMediatorExample.server;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.RestaurantTable;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.server.Handlers.*;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.*;

import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
/*
 * MULTI-THREADED SERVER ARCHITECTURE:
 * ===================================
 * 
 * This server uses the OCSF (Object Client-Server Framework) which automatically creates
 * a new thread for each client connection. This means:
 * 
 * 1. AUTOMATIC MULTI-THREADING: When a user opens the app, they get their own thread.
 *    When another user opens the app simultaneously, they get a separate thread.
 * 
 * 2. THREAD SAFETY MEASURES:
 *    - SubscribersList: Uses CopyOnWriteArrayList for thread-safe concurrent access
 *    - Client communications: Synchronized on individual client connections
 *    - Database sessions: Each operation opens its own session (thread-safe)
 *    - Hook methods: clientDisconnected and clientException are synchronized
 * 
 * 3. OCSF FRAMEWORK DETAILS:
 *    - AbstractServer.run() creates a ConnectionToClient thread for each connection
 *    - receiveMessageFromClient() is already synchronized in AbstractServer
 *    - Each client's messages are processed in their own thread
 * 
 * IMPORTANT NOTES:
 * - The server updates all clients after menu or table changes
 * - Individual clients receive their specific order updates
 * - Managers receive monthly sales and profit reports
 */

public class SimpleServer extends AbstractServer {
	// Thread-safe list for managing concurrent client subscriptions
	public static final CopyOnWriteArrayList<SubscribedClient> SubscribersList = new CopyOnWriteArrayList<>();

	public SimpleServer(int port) {
		super(port);
		DatabaseInitializer.initializeAll(); // Initialize the database with default menu items
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

	/**
	 * Hook method called when a new client connects.
	 * Logs the connection - actual client data added when they register/login.
	 */
	@Override
	protected void clientConnected(ConnectionToClient client) {
		System.out.println("New client connected from: " + client.getInetAddress().getHostAddress());
		System.out.println("Total active connections: " + getNumberOfClients());
	}

	/**
	 * Hook method called when a client disconnects.
	 * Removes the client from the subscribers list if present.
	 */
	@Override
	protected synchronized void clientDisconnected(ConnectionToClient client) {
		System.out.println("Client disconnected from: " + client.getInetAddress().getHostAddress());
		
		// Remove client from subscribers list
		SubscribersList.removeIf(sc -> sc.getClient().equals(client));
		
		System.out.println("Total active connections: " + getNumberOfClients());
	}

	/**
	 * Hook method called when an exception occurs with a client connection.
	 */
	@Override
	protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
		System.err.println("Client exception from: " + client.getInetAddress().getHostAddress());
		System.err.println("Exception: " + exception.getMessage());
		
		// Remove client from subscribers list on exception
		SubscribersList.removeIf(sc -> sc.getClient().equals(client));
	}

	/**
	 * Thread-safe method to get a client connection by phone number
	 */
	public static ConnectionToClient getClientByPhone(String phoneNumber) {
		for (SubscribedClient sc : SubscribersList) {
			if (sc.getPhoneNumber() != null && sc.getPhoneNumber().equals(phoneNumber)) {
				return sc.getClient();
			}
		}
		return null;
	}

	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		System.out.println("Received message from client: " + msg);
		String msgString = msg.toString();
		System.out.println("Received message from client: " + msgString);

        if (msgString.startsWith("GET_MENU_BY_CATEGORY")) {
            MenuHandler.sendMenuByCategoryToClient(msgString, client);
        } else if (msgString.startsWith("GET_MENU")) {
            MenuHandler.sendMenuToClient(msgString, client);
		} else if (msgString.startsWith("UPDATE_PRICE")) {
			MenuHandler.handleUpdatePriceRequest(msgString, client);
		} else if (msgString.startsWith("ADD_ITEM")) {
			MenuHandler.handleAddItemRequest(msgString, client);
		} else if (msgString.startsWith("DELETE_ITEM")) {
			MenuHandler.handleDeleteItemRequest(msgString, client);
        } else if (msgString.startsWith("TOGGLE_ITEM_SCOPE")) {
            MenuHandler.handleToggleItemScope(msgString, client);
        } else if (msgString.startsWith("UPDATE_INGREDIENTS")) {
            MenuHandler.handleUpdateIngredients(msgString, client);
		}
		//reservation
		else if (msgString.startsWith("RESERVE_TABLE")) {
			ReservationHandler.handleReserveRequest(msgString, client);
		} else if (msgString.startsWith("CANCEL_RESERVATION")) {
			ReservationHandler.handleCancelReservation(msgString, client);
		} else if (msgString.startsWith("GET_AVAILABLE_TABLES")) {
			ReservationHandler.handleGetAvailableTables(msgString,client);
		} else if (msgString.startsWith("GET_USER_RESERVATIONS")) {
			ReservationHandler.handleGetUserReservations(msgString, client);
		} else if (msgString.startsWith("GET_RESERVATIONS_FOR_TIME")) {
			ReservationHandler.handleGetReservationsForTime(msgString, client);
		} else if (msgString.startsWith("GET_BRANCH_TABLES")) {
			ReservationHandler.handleGetBranchTables(msgString, client);
		}

		//order
		else if (msgString.startsWith("CREATE_ORDER")) {
			OrderHandler.handleCreateOrder(msgString, client);
		} else if (msgString.startsWith("CANCEL_ORDER")) {
			OrderHandler.handleCancelOrder(msgString, client);
		} else if (msgString.startsWith("GET_ALL_ORDERS")) {
			OrderHandler.handleGetAllOrders(client);
		} else if (msgString.startsWith("GET_USER_ORDERS")) {
			OrderHandler.handleGetOrdersByPhoneNumber(msgString, client);
		} else if (msgString.startsWith("GET_REPORTS")) {
			ReportHandler.handleGetReports(client);
		} else if (msgString.startsWith("GENERATE_REPORT")) {
			ReportHandler.handleGenerateReport(msgString, client);
		} else if (msgString.startsWith("EXPORT_REPORT")) {
			ReportHandler.handleExportReport(msgString, client);
		}
		//client
		else if (msgString.startsWith("SIGNUP")) {
			ClientHandler.handleSignup(msgString, client);
		} else if (msgString.startsWith("LOGIN")) {
			ClientHandler.handleLogin(msgString, client);
		} else if (msgString.startsWith("REGISTER_CLIENT")) {
			ClientHandler.handleRegisterClient(msgString, client);
		} else if (msgString.startsWith("GET_CLIENT_INFO")) {
			ClientHandler.handleGetClientInfo(msgString, client);
		} else if (msgString.startsWith("UPDATE_CLIENT_EMAIL")) {
			ClientHandler.handleUpdateClientEmail(msgString, client);
		} else if (msgString.startsWith("GET_USER_ACCOUNT_INFO")) {
			ClientHandler.handleGetUserAccountInfo(msgString, client);
		} else if (msgString.startsWith("UPDATE_USER_ACCOUNT")) {
			ClientHandler.handleUpdateUserAccount(msgString, client);
		} else if (msgString.startsWith("GET_ALL_BRANCHES")) {
			handleGetAllBranches(client);
		}
		//complaint
		else if (msgString.startsWith("CREATE_COMPLAINT")) {
			ComplaintHandler.handleCreateComplaint(msgString, client);
		} else if (msgString.startsWith("GET_USER_COMPLAINTS")) {
			ComplaintHandler.handleGetUserComplaints(msgString, client);
		} else if (msgString.startsWith("CREATE_GENERAL_MESSAGE")) {
			ComplaintHandler.handleCreateGeneralMessage(msgString, client);
		} else if (msgString.startsWith("GET_USER_GENERAL_MESSAGES")) {
			ComplaintHandler.handleGetUserGeneralMessages(msgString, client);
		} else if (msgString.startsWith("GET_BRANCH_COMPLAINTS")) {
			ComplaintHandler.handleGetBranchComplaints(msgString, client);
		} else if (msgString.startsWith("GET_ALL_COMPLAINTS")) {
			ComplaintHandler.handleGetAllComplaints(msgString, client);
		} else if (msgString.startsWith("RESPOND_TO_COMPLAINT")) {
			ComplaintHandler.handleRespondToComplaint(msgString, client);
		}
	}

	public static String serializeOrders(List<Order> orders) {
		StringBuilder sb = new StringBuilder();
		for (Order order : orders) {
			sb.append(order.getId()).append(",")
					.append(order.getCustomerName()).append(",")
					.append(order.getStatus()).append(",")
					.append(order.getTotalCost()).append(",")
					.append(order.getDeliveryDate()).append(",")
					.append(order.getDeliveryTime()).append(",")
					.append(order.getDeliveryLocation()).append("|"); // Use '|' between orders
		}
		return sb.toString();
	}

	/**
	 * Handle GET_ALL_BRANCHES request - send all branches with opening hours to client
	 */
	public static void handleGetAllBranches(ConnectionToClient client) {
		try {
			SessionFactory factory = Database.getSessionFactoryInstance();
			Session session = factory.openSession();
			
			List<Branch> branches = session.createQuery("from Branch", Branch.class).getResultList();
			System.out.println("Sending " + branches.size() + " branches to client");
			
			// Send branches to client
			client.sendToClient(branches);
			session.close();
			
		} catch (IOException e) {
			System.err.println("Error sending branches to client: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Error fetching branches from database: " + e.getMessage());
			e.printStackTrace();
		}
	}
	

	/**
	 * Thread-safe method to send updated orders to a specific client
	 */
	public static void sendUpdatedOrdersToClient(String phone, ConnectionToClient client) {
		try {
			List<Order> userOrders = DataManager.fetchOrdersByPhoneNumber(phone);
			String data = serializeOrders(userOrders);
			synchronized (client) {
				client.sendToClient("order_update;" + data);
			}
			System.out.println("Sent updated orders to client with phone: " + phone);
		} catch (IOException e) {
			System.err.println("Error sending orders to client: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Thread-safe method to broadcast updated menu to all connected clients
	 */
	public static void sendUpdatedMenuToAllClients() {
		List<MenuItem> updatedItems = DataManager.fetchAll(MenuItem.class);
		System.out.println("Broadcasting updated menu to " + SubscribersList.size() + " subscribed clients");
		
		// CopyOnWriteArrayList allows safe iteration even if list is modified during iteration
		for (SubscribedClient subscribedClient : SubscribersList) {
			try {
				ConnectionToClient client = subscribedClient.getClient();
				if (client != null) {
					synchronized (client) {
						client.sendToClient(updatedItems);
					}
				}
			} catch (IOException e) {
				System.err.println("Error sending menu to client: " + e.getMessage());
			}
		}
		System.out.println("Sent updated menu to all clients.");
	}

	/**
	 * Thread-safe method to broadcast updated reservations/tables to all connected clients
	 */
	public static void sendUpdatedreservationsToAllClients() {
		List<RestaurantTable> updatedItems = DataManager.fetchAll(RestaurantTable.class);
		System.out.println("Broadcasting updated reservations to " + SubscribersList.size() + " subscribed clients");
		
		// CopyOnWriteArrayList allows safe iteration even if list is modified during iteration
		for (SubscribedClient subscribedClient : SubscribersList) {
			try {
				ConnectionToClient client = subscribedClient.getClient();
				if (client != null) {
					synchronized (client) {
						client.sendToClient(updatedItems);
					}
				}
			} catch (IOException e) {
				System.err.println("Error sending reservations to client: " + e.getMessage());
			}
		}
		System.out.println("Sent updated reservations to all clients.");
	}

	/**
	 * Thread-safe method to send success response to a client
	 */
	public static void sendSuccessResponse(ConnectionToClient client, String responseType, String message) {
		try {
			synchronized (client) {
				client.sendToClient(responseType + ";" + message);
			}
		} catch (IOException e) {
			System.err.println("Error sending success response: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Thread-safe method to send failure response to a client
	 */
	public static void sendFailureResponse(ConnectionToClient client, String responseType, String reason) {
		try {
			synchronized (client) {
				client.sendToClient(responseType + ";" + reason);
			}
		} catch (IOException e) {
			System.err.println("Error sending failure response: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
