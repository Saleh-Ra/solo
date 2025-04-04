package il.cshaifasweng.OCSFMediatorExample.server.Handlers;

import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import il.cshaifasweng.OCSFMediatorExample.server.SimpleServer;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;
import java.util.List;

public class ClientHandler {

    public static void handleRegisterClient(String msgString, ConnectionToClient client) {
        System.out.println("In handleRegisterClient with message: " + msgString);
        String[] parts = msgString.split(";");
        // Expected: "REGISTER_CLIENT;name;phone;isManager;password"

        if (parts.length < 5) {
            try {
                client.sendToClient(new Warning("SIGNUP_FAILURE: Invalid format"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String name = parts[1].trim();
        String phone = parts[2].trim();
        boolean isManager = Boolean.parseBoolean(parts[3].trim());
        String password = parts[4].trim();

        // Check if phone number already exists
        List<UserAccount> existingAccounts = DataManager.fetchByField(UserAccount.class, "phoneNumber", phone);
        if (!existingAccounts.isEmpty()) {
            try {
                client.sendToClient(new Warning("SIGNUP_FAILURE: Phone number already registered"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            // Create and save the UserAccount
            UserAccount account = new UserAccount(name, phone, false, password);
            System.out.println("About to save UserAccount: " + account.getName() + ", " + account.getPhoneNumber());
            DataManager.add(account);
            System.out.println("UserAccount saved successfully with ID: " + account.getId());

            // Create and save the Client
            Client newClient = new Client(name, account);
            System.out.println("About to save Client: " + newClient.getName());
            DataManager.add(newClient);
            System.out.println("Client saved successfully with ID: " + newClient.getId());

            // Send success response
            System.out.println("Sending success response to client");
            client.sendToClient(new Warning("SIGNUP_SUCCESS"));
        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            e.printStackTrace();
            try {
                client.sendToClient(new Warning("SIGNUP_FAILURE: " + e.getMessage()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void handleSignup(String msgString, ConnectionToClient client) {
        System.out.println("In handleSignup with message: " + msgString);
        // Expected: "SIGNUP;username;phone;password;confirmed password"
        String[] parts = msgString.split(";");
        if (parts.length < 5) {
            try {
                client.sendToClient(new Warning("SIGNUP_FAILURE: Invalid format"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String username = parts[1].trim();
        String phone = parts[2].trim();
        String password = parts[3].trim();
        String confirmedPassword = parts[4].trim();

        if (username.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmedPassword.isEmpty()) {
            try {
                client.sendToClient(new Warning("SIGNUP_FAILURE: Unfilled fields"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        
        if(!password.equals(confirmedPassword)) {
            try {
                client.sendToClient(new Warning("SIGNUP_FAILURE: Passwords do not match"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        //if everything is fine then we register
        String message = "REGISTER_CLIENT;" + username + ";" + phone + ";false;" + password;
        handleRegisterClient(message, client);
    }

    public static void handleLogin(String msgString, ConnectionToClient client) {
        // Expected: "LOGIN;username;password"
        String[] parts = msgString.split(";");
        if (parts.length < 3) {
            try {
                client.sendToClient(new Warning("LOGIN_FAILURE: Invalid format"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String username = parts[1].trim();
        String password = parts[2].trim();

        if (username.isEmpty() || password.isEmpty()) {
            try {
                client.sendToClient(new Warning("LOGIN_FAILURE: Username and password are required"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Query the database to check credentials using DataManager
        List<UserAccount> accounts = DataManager.fetchByField(UserAccount.class, "name", username);
        
        if (accounts.isEmpty()) {
            try {
                client.sendToClient(new Warning("LOGIN_FAILURE: Invalid username or password"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        UserAccount account = accounts.get(0);
        if (!account.getPassword().equals(password)) {
            try {
                client.sendToClient(new Warning("LOGIN_FAILURE: Invalid username or password"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Login successful
        try {
            client.sendToClient(new Warning("LOGIN_SUCCESS"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleGetClientInfo(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        // Expected: "GET_CLIENT_INFO;name;phone"

        if (parts.length < 2) {
            try {
                client.sendToClient(new Warning("GET_CLIENT_INFO_FAILURE: Invalid format"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            try {
                client.sendToClient(new Warning("GET_CLIENT_INFO_FAILURE: Invalid ID"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }

        List<Client> clients = DataManager.fetchByField(Client.class, "account.id", userId);
        if (clients.isEmpty()) {
            try {
                client.sendToClient(new Warning("GET_CLIENT_INFO_FAILURE: Client not found"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            client.sendToClient(clients.get(0));
        } catch (IOException e) {
            e.printStackTrace();
            try {
                client.sendToClient(new Warning("GET_CLIENT_INFO_FAILURE: Error sending client data"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void handleUpdateClientEmail(String msgString, ConnectionToClient client) {
        // TODO: Update email by ID or name
        System.out.println("Received client email update request: " + msgString);
    }
}
