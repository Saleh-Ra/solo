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
		}
	}





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
