package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SimpleServer extends AbstractServer {
	private static final ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();

	public SimpleServer(int port) {
		super(port);
		DatabaseInitializer.initializeAll();
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
			MenuHandler.sendMenuToClient(client);
		} else if (msgString.startsWith("UPDATE_PRICE")) {
			MenuHandler.handleUpdatePriceRequest(msgString, client);
		} else if (msgString.startsWith("ADD_ITEM")) {
			MenuHandler.handleAddItemRequest(msgString, client);
		} else if (msgString.startsWith("PLACE_ORDER")) {
			handlePlaceOrderRequest(msgString, client);
		} else if (msgString.startsWith("DELIVERY_ORDER")) {
			handleDeliveryOrderRequest(msgString, client);  // 👈 New delivery handler
		}
	}

	private void handlePlaceOrderRequest(String msgString, ConnectionToClient client) {
		try {
			String[] parts = msgString.split(";");
			int clientId = Integer.parseInt(parts[1]);
			int branchId = Integer.parseInt(parts[2]);
			String[] itemIds = parts[3].split(",");

			Client customer = DataManager.find(Client.class, clientId);
			if (customer == null) {
				sendFailureResponse(client, "ORDER_FAILURE", "Client not found");
				return;
			}

			double totalCost = 0;
			for (String idStr : itemIds) {
				int id = Integer.parseInt(idStr.trim());
				MenuItem item = DataManager.find(MenuItem.class, id);
				if (item == null) {
					sendFailureResponse(client, "ORDER_FAILURE", "Menu item not found: " + id);
					return;
				}
				totalCost += item.getPrice();
			}

			Order order = new Order(branchId, totalCost, "Pending", customer);
			DataManager.save(order);

			sendSuccessResponse(client, "ORDER_SUCCESS", "Order placed successfully");

		} catch (Exception e) {
			e.printStackTrace();
			sendFailureResponse(client, "ORDER_FAILURE", "An error occurred: " + e.getMessage());
		}
	}

	// ✅ NEW: Handle delivery order requests
	private void handleDeliveryOrderRequest(String msgString, ConnectionToClient client) {
		try {
			String[] parts = msgString.split(";");
			int clientId = Integer.parseInt(parts[1]);
			int orderId = Integer.parseInt(parts[2]);
			String deliveryAddress = parts[3];

			Client customer = DataManager.find(Client.class, clientId);
			Order order = DataManager.find(Order.class, orderId);

			if (customer == null) {
				sendFailureResponse(client, "DELIVERY_FAILURE", "Client not found");
				return;
			}

			if (order == null) {
				sendFailureResponse(client, "DELIVERY_FAILURE", "Order not found");
				return;
			}

			Delivery delivery = new Delivery(
					clientId,
					orderId,
					deliveryAddress,
					new Date(),
					"Pending"
			);

			DataManager.save(delivery);
			sendSuccessResponse(client, "DELIVERY_SUCCESS", "Delivery order placed successfully");

		} catch (Exception e) {
			e.printStackTrace();
			sendFailureResponse(client, "DELIVERY_FAILURE", "An error occurred: " + e.getMessage());
		}
	}

	// ------------------ UTILITIES ------------------

	protected static void sendUpdatedMenuToAllClients() {
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
