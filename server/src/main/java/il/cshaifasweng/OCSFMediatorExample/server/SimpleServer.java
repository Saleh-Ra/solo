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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/*VERY IMPORTANT:
the server has to update the clients after each change in tables or menu change.
it also has to update one client regarding the orders he has.
in addition, it must send messages to the managers regarding monthly sales and profit.
*/

public class SimpleServer extends AbstractServer {
	// Thread-safe list for managing client subscriptions
	public static final List<SubscribedClient> SubscribersList = Collections.synchronizedList(new ArrayList<>());
	
	// Thread pool for handling heavy operations asynchronously
	private static final ExecutorService taskExecutor = Executors.newFixedThreadPool(10);
	
	// Thread pool for database operations
	private static final ExecutorService dbExecutor = Executors.newFixedThreadPool(5);

	public SimpleServer(int port) {
		super(port);
		DatabaseInitializer.initializeAll(); // Initialize the database with default menu items
	}

	public static void main(String[] args) {
		SimpleServer server = new SimpleServer(3000);
		
		// Add shutdown hook to gracefully close thread pools
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Server shutting down...");
			shutdownThreadPools();
		}));
		
		try {
			server.listen();
			System.out.println("Server started and listening on port 3000");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static ConnectionToClient getClientByPhone(String phoneNumber) {
		for (SubscribedClient sc : SubscribersList) {
		if (sc.getPhoneNumber().equals(phoneNumber)) {
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

		// Send immediate acknowledgment for heavy operations
		if (isHeavyOperation(msgString)) {
			try {
				client.sendToClient("REQUEST_RECEIVED");
			} catch (IOException e) {
				System.err.println("Error sending acknowledgment: " + e.getMessage());
			}
		}

        if (msgString.startsWith("GET_MENU_BY_CATEGORY")) {
            MenuHandler.sendMenuByCategoryToClient(msgString, client);
        } else if (msgString.startsWith("GET_MENU")) {
            MenuHandler.sendMenuToClient(msgString, client);
		} else if (msgString.startsWith("UPDATE_PRICE")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> MenuHandler.handleUpdatePriceRequest(msgString, client));
		} else if (msgString.startsWith("ADD_ITEM")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> MenuHandler.handleAddItemRequest(msgString, client));
		} else if (msgString.startsWith("DELETE_ITEM")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> MenuHandler.handleDeleteItemRequest(msgString, client));
        } else if (msgString.startsWith("TOGGLE_ITEM_SCOPE")) {
            // Heavy operation - process asynchronously
            taskExecutor.submit(() -> MenuHandler.handleToggleItemScope(msgString, client));
        } else if (msgString.startsWith("UPDATE_INGREDIENTS")) {
            // Heavy operation - process asynchronously
            taskExecutor.submit(() -> MenuHandler.handleUpdateIngredients(msgString, client));
		}
		//reservation
		else if (msgString.startsWith("RESERVE_TABLE")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> ReservationHandler.handleReserveRequest(msgString, client));
		} else if (msgString.startsWith("CANCEL_RESERVATION")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> ReservationHandler.handleCancelReservation(msgString, client));
		} else if (msgString.startsWith("GET_AVAILABLE_TABLES")) {
			ReservationHandler.handleGetAvailableTables(msgString,client);
		}

		//order
		else if (msgString.startsWith("CREATE_ORDER")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> OrderHandler.handleCreateOrder(msgString, client));
		} else if (msgString.startsWith("CANCEL_ORDER")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> OrderHandler.handleCancelOrder(msgString, client));
		} else if (msgString.startsWith("GET_ALL_ORDERS")) {
			OrderHandler.handleGetAllOrders(client);
		} else if (msgString.startsWith("GET_USER_ORDERS")) {
			OrderHandler.handleGetOrdersByPhoneNumber(msgString, client);
		} else if (msgString.startsWith("GET_REPORTS")) {
			ReportHandler.handleGetReports(client);
		} else if (msgString.startsWith("GENERATE_REPORT")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> ReportHandler.handleGenerateReport(msgString, client));
		} else if (msgString.startsWith("EXPORT_REPORT")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> ReportHandler.handleExportReport(msgString, client));
		}
		//client
		else if (msgString.startsWith("SIGNUP")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> ClientHandler.handleSignup(msgString, client));
		} else if (msgString.startsWith("LOGIN")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> ClientHandler.handleLogin(msgString, client));
		} else if (msgString.startsWith("REGISTER_CLIENT")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> ClientHandler.handleRegisterClient(msgString, client));
		} else if (msgString.startsWith("GET_CLIENT_INFO")) {
			ClientHandler.handleGetClientInfo(msgString, client);
		} else if (msgString.startsWith("UPDATE_CLIENT_EMAIL")) {
			// Heavy operation - process asynchronously
			taskExecutor.submit(() -> ClientHandler.handleUpdateClientEmail(msgString, client));
		} else if (msgString.startsWith("GET_ALL_BRANCHES")) {
			handleGetAllBranches(client);
		}
	}
	
	/**
	 * Determines if an operation is heavy and should be processed asynchronously
	 */
	private boolean isHeavyOperation(String msgString) {
		return msgString.startsWith("RESERVE_TABLE") ||
			   msgString.startsWith("CANCEL_RESERVATION") ||
			   msgString.startsWith("CREATE_ORDER") ||
			   msgString.startsWith("CANCEL_ORDER") ||
			   msgString.startsWith("UPDATE_PRICE") ||
			   msgString.startsWith("ADD_ITEM") ||
			   msgString.startsWith("DELETE_ITEM") ||
			   msgString.startsWith("TOGGLE_ITEM_SCOPE") ||
			   msgString.startsWith("UPDATE_INGREDIENTS") ||
			   msgString.startsWith("GENERATE_REPORT") ||
			   msgString.startsWith("EXPORT_REPORT") ||
			   msgString.startsWith("SIGNUP") ||
			   msgString.startsWith("LOGIN") ||
			   msgString.startsWith("REGISTER_CLIENT") ||
			   msgString.startsWith("UPDATE_CLIENT_EMAIL");
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

	public static void sendUpdatedOrdersToClient(String phone, ConnectionToClient client) {
		try {
			List<Order> userOrders = DataManager.fetchOrdersByPhoneNumber(phone); // You'll need this query if not defined
			String data = serializeOrders(userOrders);
			client.sendToClient("order_update;" + data);
			System.out.println("Sent updated orders to client with phone: " + phone);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendUpdatedMenuToAllClients() {
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

	//this method is not for messages but for updating every client about what is going on.
	public static void sendUpdatedreservationsToAllClients() {
		List<RestaurantTable> updatedItems = DataManager.fetchAll(RestaurantTable.class);
		try {
			for (SubscribedClient subscribedClient : SubscribersList) {
				subscribedClient.getClient().sendToClient(updatedItems);
			}
			System.out.println("Sent updated menu to all clients.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendSuccessResponse(ConnectionToClient client, String responseType, String message) {
		try {
			client.sendToClient(responseType + ";" + message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendFailureResponse(ConnectionToClient client, String responseType, String reason) {
		try {
			client.sendToClient(responseType + ";" + reason);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Gracefully shuts down the thread pools when the server is closing
	 */
	public static void shutdownThreadPools() {
		System.out.println("Shutting down thread pools...");
		
		// Shutdown task executor
		taskExecutor.shutdown();
		try {
			if (!taskExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
				taskExecutor.shutdownNow();
				if (!taskExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
					System.err.println("Task executor did not terminate");
				}
			}
		} catch (InterruptedException e) {
			taskExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}
		
		// Shutdown database executor
		dbExecutor.shutdown();
		try {
			if (!dbExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
				dbExecutor.shutdownNow();
				if (!dbExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
					System.err.println("Database executor did not terminate");
				}
			}
		} catch (InterruptedException e) {
			dbExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}
		
		System.out.println("Thread pools shut down successfully");
	}
	
	/**
	 * Get the database executor for use in handlers
	 */
	public static ExecutorService getDbExecutor() {
		return dbExecutor;
	}
	
	/**
	 * Get the task executor for use in handlers
	 */
	public static ExecutorService getTaskExecutor() {
		return taskExecutor;
	}
}
