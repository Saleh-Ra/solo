package il.cshaifasweng.OCSFMediatorExample.server.Handlers;

import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.server.SimpleServer;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler {

    public static void handleRegisterClient(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        // Expected: "REGISTER_CLIENT;username;password;phone;name;contactEmail"

        if (parts.length < 4) {
            SimpleServer.sendFailureResponse(client, "REGISTER_CLIENT_FAILURE", "Invalid format");
            return;
        }

        int id=Integer.parseInt(parts[1]);
        String phone = parts[2].trim();
        String name = parts[3].trim();
        String contactEmail = parts[4].trim();

        // Check if username already exists
        List<UserAccount> existingAccounts = DataManager.fetchByField(UserAccount.class, "id", id);
        if (!existingAccounts.isEmpty()) {
            SimpleServer.sendFailureResponse(client, "REGISTER_CLIENT_FAILURE", "Username already exists");
            return;
        }

        // Create the account and client
        UserAccount account = new UserAccount(phone, false);
        DataManager.add(account);

        // Refetch to get the account with ID assigned by DB
        UserAccount savedAccount = DataManager.fetchByField(UserAccount.class, "id", id).get(0);
        Client newClient = new Client(name, contactEmail, savedAccount);
        DataManager.add(newClient);

        SimpleServer.sendSuccessResponse(client, "REGISTER_CLIENT_SUCCESS", "Client registered");
    }


    public static void handleGetClientInfo(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        // Expected: "GET_CLIENT_INFO;userAccountId"

        if (parts.length < 2) {
            SimpleServer.sendFailureResponse(client, "GET_CLIENT_INFO_FAILURE", "Invalid format");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            SimpleServer.sendFailureResponse(client, "GET_CLIENT_INFO_FAILURE", "Invalid ID");
            return;
        }

        List<Client> clients = DataManager.fetchByField(Client.class, "account.id", userId);
        if (clients.isEmpty()) {
            SimpleServer.sendFailureResponse(client, "GET_CLIENT_INFO_FAILURE", "Client not found");
            return;
        }

        try {
            client.sendToClient(clients.get(0));
        } catch (IOException e) {
            e.printStackTrace();
            SimpleServer.sendFailureResponse(client, "GET_CLIENT_INFO_FAILURE", "Error sending client data");
        }
    }

    /*public static void handleLogin(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        // Expected: "LOGIN;username;password"

        if (parts.length < 3) {
            SimpleServer.sendFailureResponse(client, "LOGIN_FAILURE", "Invalid format");
            return;
        }

        String username = parts[1].trim();
        String password = parts[2].trim();

        List<UserAccount> accounts = DataManager.fetchByField(UserAccount.class, "username", username);
        if (accounts.isEmpty()) {
            SimpleServer.sendFailureResponse(client, "LOGIN_FAILURE", "Username not found");
            return;
        }

        UserAccount account = accounts.get(0);
        if (!account.getPassword().equals(password)) {
            SimpleServer.sendFailureResponse(client, "LOGIN_FAILURE", "Incorrect password");
            return;
        }

        // Optional: send back info about the user
        String userType = account.isManager() ? "manager" : "client";
        SimpleServer.sendSuccessResponse(client, "LOGIN_SUCCESS", userType + ";" + account.getId());
    }*/

    public static void handleUpdateClientEmail(String msgString, ConnectionToClient client) {
        // TODO: Update email by ID or name
        System.out.println("Received client email update request: " + msgString);
    }
}
