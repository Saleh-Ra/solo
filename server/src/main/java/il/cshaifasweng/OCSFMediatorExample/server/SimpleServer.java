package il.cshaifasweng.OCSFMediatorExample.server;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.RestaurantTable;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.server.Handlers.*;
import il.cshaifasweng.OCSFMediatorExample.server.Handlers.ComplaintHandler;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.*;

import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
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
	 * Sets the user account to offline.
	 */
	@Override
	protected synchronized void clientDisconnected(ConnectionToClient client) {
		System.out.println("Client disconnected from: " + client.getInetAddress().getHostAddress());
		
		// Set user account to offline if logged in
		setAccountOffline(client);
		
		// Remove client from subscribers list
		SubscribersList.removeIf(sc -> sc.getClient().equals(client));
		
		System.out.println("Total active connections: " + getNumberOfClients());
	}

	/**
	 * Hook method called when an exception occurs with a client connection.
	 * Sets the user account to offline.
	 */
	@Override
	protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
		System.err.println("Client exception from: " + client.getInetAddress().getHostAddress());
		System.err.println("Exception: " + exception.getMessage());
		
		// Set user account to offline if logged in
		setAccountOffline(client);
		
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

        // Handle client registration/subscription
        if (msgString.equalsIgnoreCase("add client")) {
            // Ensure client is in SubscribersList
            boolean alreadySubscribed = SubscribersList.stream()
                .anyMatch(sc -> sc.getClient().equals(client));
            if (!alreadySubscribed) {
                SubscribedClient subscribedClient = new SubscribedClient(client);
                SubscribersList.add(subscribedClient);
                System.out.println("Client added to SubscribersList");
            }
            return;
        }
        
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
		} else if (msgString.startsWith("MARK_ORDER_READY")) {
			OrderHandler.handleMarkOrderReady(msgString, client);
		} else if (msgString.startsWith("GET_ALL_ORDERS")) {
			OrderHandler.handleGetAllOrders(client);
		} else if (msgString.startsWith("GET_USER_ORDERS")) {
			OrderHandler.handleGetOrdersByPhoneNumber(msgString, client);
		} else if (msgString.startsWith("GET_UPCOMING_ORDERS")) {
			OrderHandler.handleGetUpcomingOrders(client);
		} else if (msgString.startsWith("GET_UPCOMING_RESERVATIONS")) {
			ReservationHandler.handleGetUpcomingReservations(client);
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
		} else if (msgString.startsWith("LOGOUT")) {
			ClientHandler.handleLogout(msgString, client);
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
		} else if (msgString.startsWith("FORGOT_PASSWORD_RESET")) {
			ClientHandler.handleForgotPasswordReset(msgString, client);
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
	 * Reuses the same logic as GET_MENU to respect each client's branch selection
	 */
	public static void sendUpdatedMenuToAllClients() {
		System.out.println("Broadcasting updated menu to " + SubscribersList.size() + " subscribed clients");
		
		// CopyOnWriteArrayList allows safe iteration even if list is modified during iteration
		for (SubscribedClient subscribedClient : SubscribersList) {
			try {
				ConnectionToClient client = subscribedClient.getClient();
				if (client != null) {
					// Get the branchId this client is viewing (stored when they requested menu)
					Integer branchId = subscribedClient.getBranchId();
					
					// Reuse the same logic as GET_MENU - respects branch_id!
					String menuRequest = (branchId != null) ? "GET_MENU;" + branchId : "GET_MENU";
					MenuHandler.sendMenuToClient(menuRequest, client);
				}
			} catch (Exception e) {
				System.err.println("Error sending menu to client: " + e.getMessage());
				e.printStackTrace();
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
	 * Thread-safe method to broadcast updated orders to all workers
	 * Sends upcoming orders (Pending/Preparing) to all connected clients
	 * Workers will see them in their personal area, other clients will ignore
	 */
	public static void sendUpdatedOrdersToAllWorkers() {
		System.out.println("📢 Broadcasting updated orders to " + SubscribersList.size() + " subscribed clients");
		
		// CopyOnWriteArrayList allows safe iteration even if list is modified during iteration
		int sentCount = 0;
		for (SubscribedClient subscribedClient : SubscribersList) {
			try {
				ConnectionToClient client = subscribedClient.getClient();
				if (client != null) {
					// Send upcoming orders (same logic as GET_UPCOMING_ORDERS)
					OrderHandler.handleGetUpcomingOrders(client);
					sentCount++;
					System.out.println("📢 Sent updated orders to client " + sentCount);
				} else {
					System.out.println("⚠️ Skipping null client in broadcast");
				}
			} catch (Exception e) {
				System.err.println("❌ Error sending orders to client: " + e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println("✅ Broadcast complete: sent updated orders to " + sentCount + " clients");
	}

	/**
	 * Thread-safe method to broadcast updated reservations to all workers
	 * Sends upcoming reservations to all connected clients
	 * Workers will see them in their personal area, other clients will ignore
	 */
	public static void sendUpdatedReservationsToAllWorkers() {
		System.out.println("📢 Broadcasting updated reservations to " + SubscribersList.size() + " subscribed clients");
		
		// CopyOnWriteArrayList allows safe iteration even if list is modified during iteration
		int sentCount = 0;
		for (SubscribedClient subscribedClient : SubscribersList) {
			try {
				ConnectionToClient client = subscribedClient.getClient();
				if (client != null) {
					// Send upcoming reservations (same logic as GET_UPCOMING_RESERVATIONS)
					ReservationHandler.handleGetUpcomingReservations(client);
					sentCount++;
					System.out.println("📢 Sent updated reservations to client " + sentCount);
				} else {
					System.out.println("⚠️ Skipping null client in broadcast");
				}
			} catch (Exception e) {
				System.err.println("❌ Error sending reservations to client: " + e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println("✅ Broadcast complete: sent updated reservations to " + sentCount + " clients");
	}

	/**
	 * Thread-safe method to broadcast updated complaints to managers
	 * Sends branch-specific complaints to branch managers, or all complaints to chain managers
	 */
	public static void sendUpdatedComplaintsToManagers(int branchId) {
		System.out.println("📢 Broadcasting updated complaints for branch " + branchId + " to " + SubscribersList.size() + " subscribed clients");
		
		int sentCount = 0;
		for (SubscribedClient subscribedClient : SubscribersList) {
			try {
				ConnectionToClient client = subscribedClient.getClient();
				if (client != null) {
					// Send branch-specific complaints (managers will filter on client side)
					ComplaintHandler.handleGetBranchComplaints("GET_BRANCH_COMPLAINTS;" + branchId, client);
					sentCount++;
					System.out.println("📢 Sent updated complaints to manager client " + sentCount);
				}
			} catch (Exception e) {
				System.err.println("❌ Error sending complaints to manager: " + e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println("✅ Broadcast complete: sent updated complaints to " + sentCount + " manager clients");
	}

	/**
	 * Thread-safe method to broadcast updated complaints to all managers (chain manager view)
	 */
	public static void sendUpdatedComplaintsToAllManagers() {
		System.out.println("📢 Broadcasting all updated complaints to " + SubscribersList.size() + " subscribed clients");
		
		int sentCount = 0;
		for (SubscribedClient subscribedClient : SubscribersList) {
			try {
				ConnectionToClient client = subscribedClient.getClient();
				if (client != null) {
					// Send all complaints (for chain managers)
					ComplaintHandler.handleGetAllComplaints("GET_ALL_COMPLAINTS", client);
					sentCount++;
					System.out.println("📢 Sent all complaints to manager client " + sentCount);
				}
			} catch (Exception e) {
				System.err.println("❌ Error sending all complaints to manager: " + e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println("✅ Broadcast complete: sent all complaints to " + sentCount + " manager clients");
	}

	/**
	 * Thread-safe method to send updated complaints to a specific customer
	 */
	public static void sendUpdatedComplaintsToCustomer(String phoneNumber) {
		System.out.println("📢 Sending updated complaints to customer: " + phoneNumber);
		
		// Find the client connection by phone number
		ConnectionToClient client = getClientByPhone(phoneNumber);
		if (client != null) {
			try {
				ComplaintHandler.handleGetUserComplaints("GET_USER_COMPLAINTS;" + phoneNumber, client);
				System.out.println("✅ Sent updated complaints to customer: " + phoneNumber);
			} catch (Exception e) {
				System.err.println("❌ Error sending complaints to customer: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			System.out.println("⚠️ Customer not connected: " + phoneNumber);
		}
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

	/**
	 * Helper method to set a user account to offline when client disconnects
	 */
	private void setAccountOffline(ConnectionToClient client) {
		try {
			Object userObj = client.getInfo("user");
			if (userObj instanceof UserAccount) {
				UserAccount account = (UserAccount) userObj;
				String phoneNumber = account.getPhoneNumber();
				
				// Reload account from database to ensure we have the latest version
				Session session = Database.getSessionFactoryInstance().openSession();
				try {
					session.beginTransaction();
					
					// Fetch the account by phone number to get the latest version
					List<UserAccount> accounts = DataManager.fetchUserAccountsByPhoneNumber(phoneNumber);
					if (!accounts.isEmpty()) {
						UserAccount dbAccount = accounts.get(0);
						dbAccount.setOnline(false);
						session.merge(dbAccount);
						session.getTransaction().commit();
						System.out.println("Account set to offline: " + phoneNumber);
					} else {
						System.out.println("Account not found in database for phone: " + phoneNumber);
						if (session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					}
				} catch (Exception ex) {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
					throw ex;
				} finally {
					if (session != null && session.isOpen()) {
						session.close();
					}
				}
			} else {
				System.out.println("No user account found in client connection info");
			}
		} catch (Exception e) {
			System.err.println("Error setting account to offline: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to reset all accounts to offline status
	 */
	private void resetAllAccountsToOffline() {
		try {
			Session session = Database.getSessionFactoryInstance().openSession();
			try {
				session.beginTransaction();
				
				// Fetch all user accounts
				List<UserAccount> accounts = session.createQuery("from UserAccount", UserAccount.class).getResultList();
				
				// Set all accounts to offline
				for (UserAccount account : accounts) {
					account.setOnline(false);
					session.merge(account);
				}
				
				session.getTransaction().commit();
				System.out.println("Reset " + accounts.size() + " accounts to offline status");
			} catch (Exception ex) {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
				throw ex;
			} finally {
				if (session != null && session.isOpen()) {
					session.close();
				}
			}
		} catch (Exception e) {
			System.err.println("Error resetting accounts to offline: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Hook method called when the server starts listening.
	 * Resets all user accounts to offline status on startup.
	 */
	@Override
	protected synchronized void serverStarted() {
		System.out.println("Server is starting. Resetting all accounts to offline...");
		resetAllAccountsToOffline();
		super.serverStarted();
	}

	/**
	 * Hook method called when the server is closed.
	 * Resets all user accounts to offline status.
	 */
	@Override
	protected synchronized void serverClosed() {
		System.out.println("Server is shutting down. Resetting all accounts to offline...");
		resetAllAccountsToOffline();
		
		// Call parent implementation
		super.serverClosed();
	}
}
