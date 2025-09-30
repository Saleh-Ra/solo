package il.cshaifasweng.OCSFMediatorExample.server;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.Reservation;
import il.cshaifasweng.OCSFMediatorExample.entities.RestaurantTable;
import il.cshaifasweng.OCSFMediatorExample.server.Handlers.*;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.*;

import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/*VERY IMPORTANT:
the server has to update the clients after each change in tables or menu change.
it also has to update one client regarding the orders he has.
in addition, it must send messages to the managers regarding monthly sales and profit.
*/

public class SimpleServer extends AbstractServer {
	public static final ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();

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
}
