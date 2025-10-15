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
        // Expected: "REGISTER_CLIENT;name;phone;role;password"
        String[] parts = msgString.split(";");
        if (parts.length < 5) {
            try {
                client.sendToClient(new Warning("REGISTER_CLIENT_FAILURE: Invalid format"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String name = parts[1].trim();
        String phone = parts[2].trim();
        String role = parts[3].trim();
        String password = parts[4].trim();

        // Validate required fields
        if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            try {
                client.sendToClient(new Warning("REGISTER_CLIENT_FAILURE: All fields are required"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Ensure role is valid, default to "client" if not specified or invalid
        if (role.isEmpty() || (!role.equalsIgnoreCase("client") && !role.equalsIgnoreCase("manager"))) {
            role = "client";
        }

        // Check if the user already exists by phone number
        List<UserAccount> existingAccounts = DataManager.fetchByField(UserAccount.class, "phoneNumber", phone);
        if (!existingAccounts.isEmpty()) {
            try {
                client.sendToClient(new Warning("REGISTER_CLIENT_FAILURE: Phone number already registered"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Create new user account
        try {
            UserAccount account = new UserAccount(name, phone, role, password);
            // For clients, branch fields remain null by default (set in constructor)
            DataManager.add(account);
            System.out.println("Created new account: " + name + " with role: " + role);

            // Create client entity if role is "client"
            if (role.equalsIgnoreCase("client")) {
                Client newClient = new Client(name, account);
                DataManager.add(newClient);
                System.out.println("Created new client: " + name);
            }

            // Success response
            client.sendToClient(new Warning("REGISTER_CLIENT_SUCCESS"));
        } catch (Exception e) {
            try {
                client.sendToClient(new Warning("REGISTER_CLIENT_FAILURE: " + e.getMessage()));
                e.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void handleSignup(String msgString, ConnectionToClient client) {
        // Expected: "SIGNUP;username;phone;password;confirmPassword"
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
        String message = "REGISTER_CLIENT;" + username + ";" + phone + ";client;" + password;
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
        // Username MUST be phone number (unique identifier)
        List<UserAccount> accounts = DataManager.fetchUserAccountsByPhoneNumber(username);
        
        if (accounts.isEmpty()) {
            try {
                client.sendToClient(new Warning("LOGIN_FAILURE: Invalid phone number or password"));
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

        // Store the user account in the client connection for future use
        client.setInfo("user", account);

        // Login successful - include phone number, role, and branch name in success message
        try {
            String phoneNumber = account.getPhoneNumber();
            String role = account.getRole();
            String branchName = account.getBranchName(); // This might be null for non-managers
            
            StringBuilder successMessage = new StringBuilder("LOGIN_SUCCESS;")
                .append(phoneNumber).append(";")
                .append(role);
            
            // Add branch name for managers if available
            if ("manager".equalsIgnoreCase(role) && branchName != null) {
                successMessage.append(";").append(branchName);
            }
            
            client.sendToClient(new Warning(successMessage.toString()));
            System.out.println("User logged in: " + username + " with phone: " + phoneNumber + 
                ", role: " + role + (branchName != null ? ", branch: " + branchName : ""));
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
