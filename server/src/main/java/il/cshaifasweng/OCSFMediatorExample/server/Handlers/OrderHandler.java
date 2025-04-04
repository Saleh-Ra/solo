package il.cshaifasweng.OCSFMediatorExample.server.Handlers;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.server.SimpleServer;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.util.List;

public class OrderHandler {
    //this method here will be called only after the customer finds a good time, otherwise this should not be called
    //the reason to that is that this method will only add to the database, it will not check any details
    public static void handleCreateOrder(String msgString, ConnectionToClient client) {

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String status = "Arrived at " + now.format(formatter);
        String[] parts = msgString.split(";");
        //msg is {command; branch id; cost; phone}
        if (parts.length < 4) {
            SimpleServer.sendFailureResponse(client, "ADD_ITEM_FAILURE", "Invalid format");
            return;
        }
        //parts[3] should include a Client's id
        List<Client> clients = DataManager.fetchByField(Client.class, "account.phoneNumber", Integer.parseInt(parts[3]));
        if (clients.isEmpty()) {
            SimpleServer.sendFailureResponse(client, "CREATE_ORDER_FAILURE", "Client not found");
            return;
        }
        Client foundClient = clients.get(0);
        Order order = new Order(Integer.parseInt(parts[1]),Double.parseDouble(parts[2]),now,foundClient);
        DataManager.add(order);
        foundClient.getOrders().add(order);
        System.out.println("Received order creation request: " + msgString);
    }


    public static void handleCancelOrder(String msgString, ConnectionToClient client) {
        //the message
        String[] parts = msgString.split(";");
        if (parts.length < 2) {
            SimpleServer.sendFailureResponse(client, "CREATE_ITEM_FAILURE", "Invalid format");
            return;
        }
        List<Order> orders = DataManager.fetchByField(Order.class, "id", Integer.parseInt(parts[1]));
        if (orders.isEmpty()) {
            SimpleServer.sendFailureResponse(client, "CANCEL_ORDER_FAILURE", "Order not found");
            return;
        }
        Order order = orders.get(0);
        LocalDateTime now = LocalDateTime.now();

        Duration diff = Duration.between(now, order.getOrderTime());
        long minutesBefore = diff.toMinutes();

        double refundPercentage;
        if (minutesBefore >= 60) {
            refundPercentage = 1.0; // Full refund
        } else if (minutesBefore >= 15) {
            refundPercentage = 0.5; // Half refund
        } else {
            refundPercentage = 0.0; // No refund
        }

        // TODO: Process refund (optional, maybe just a print for now)
        //System.out.println("Refund for client: " + (refundPercentage * 100) + "%");
        Client customer=order.getClient();
        customer.getOrders().remove(order);
        DataManager.delete(order);

        SimpleServer.sendSuccessResponse(client, "CANCEL_ORDER_SUCCESS",
                "Order cancelled. Refund: " + (int)(refundPercentage * 100) + "%");
        System.out.println("Received order cancel request: " + msgString);
    }


    public static void handleGetAllOrders(ConnectionToClient client) {
        try {
            List<Order> orders = DataManager.fetchAll(Order.class);
            client.sendToClient(orders);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
