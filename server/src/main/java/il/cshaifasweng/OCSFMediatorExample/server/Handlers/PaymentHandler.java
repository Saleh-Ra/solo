package il.cshaifasweng.OCSFMediatorExample.server.Handlers;

import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.Payment;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.SimpleServer;

import java.io.IOException;
import java.util.List;

public class PaymentHandler {

    public static void handleAddPaymentRequest(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        if (parts.length < 5) {
            SimpleServer.sendFailureResponse(client, "ADD_PAYMENT_FAILURE", "Invalid format");
            return;
        }

        try {
            int clientId = Integer.parseInt(parts[1]);
            double amount = Double.parseDouble(parts[2]);
            String method = parts[3];
            String status = parts[4]; // e.g. "Pending", "Paid"

            // ✅ Retrieve Client by ID
            List<Client> matchedClients = DataManager.fetchByField(Client.class, "id", clientId);
            if (matchedClients.isEmpty()) {
                SimpleServer.sendFailureResponse(client, "ADD_PAYMENT_FAILURE", "Client not found");
                return;
            }

            Client paymentClient = matchedClients.get(0);
            Payment payment = new Payment(amount, method, status, paymentClient);
            DataManager.add(payment);

            SimpleServer.sendSuccessResponse(client, "ADD_PAYMENT_SUCCESS", "Payment added");
        } catch (NumberFormatException e) {
            SimpleServer.sendFailureResponse(client, "ADD_PAYMENT_FAILURE", "Invalid number format");
        } catch (Exception e) {
            SimpleServer.sendFailureResponse(client, "ADD_PAYMENT_FAILURE", "Unexpected error: " + e.getMessage());
        }
    }

    public static void handleGetAllPayments(ConnectionToClient client) {
        try {
            List<Payment> payments = DataManager.fetchAll(Payment.class);
            client.sendToClient(payments);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // You can add more as needed: handleDeletePayment, handleUpdateStatus, etc.
}