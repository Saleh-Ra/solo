package il.cshaifasweng.OCSFMediatorExample.server.Handlers;

import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import il.cshaifasweng.OCSFMediatorExample.entities.Worker;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.Database;
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

            // Success response - send SIGNUP_SUCCESS to trigger client redirect
            client.sendToClient(new Warning("SIGNUP_SUCCESS;Account created successfully! Please sign in with your phone number."));
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

        // Check if account is already online (prevent duplicate logins)
        if (account.isOnline()) {
            try {
                client.sendToClient(new Warning("LOGIN_FAILURE: This account is already logged in on another device"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Set account to online and update database
        try {
            account.setOnline(true);
            org.hibernate.Session session = Database.getSessionFactoryInstance().openSession();
            try {
                session.beginTransaction();
                session.merge(account);
                session.getTransaction().commit();
                System.out.println("Account set to online: " + account.getPhoneNumber());
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
            System.err.println("Error setting account to online: " + e.getMessage());
            e.printStackTrace();
            try {
                client.sendToClient(new Warning("LOGIN_FAILURE: Error updating account status"));
            } catch (IOException ex) {
                ex.printStackTrace();
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
            
            // For workers, get branch from Worker entity
            if ("worker".equalsIgnoreCase(role) && branchName == null) {
                try (org.hibernate.Session session = Database.getSessionFactoryInstance().openSession()) {
                    session.beginTransaction();
                    String hql = "FROM Worker w WHERE w.account.id = :accountId";
                    List<Worker> workers = session.createQuery(hql, Worker.class)
                            .setParameter("accountId", account.getId())
                            .getResultList();
                    if (!workers.isEmpty() && workers.get(0).getBranch() != null) {
                        branchName = workers.get(0).getBranch().getLocation();
                    }
                    session.getTransaction().commit();
                } catch (Exception e) {
                    System.err.println("Error fetching worker branch: " + e.getMessage());
                }
            }
            
            StringBuilder successMessage = new StringBuilder("LOGIN_SUCCESS;")
                .append(phoneNumber).append(";")
                .append(role);
            
            // Add branch name for managers and workers if available
            if (("manager".equalsIgnoreCase(role) || "worker".equalsIgnoreCase(role)) && branchName != null) {
                successMessage.append(";").append(branchName);
            }
            
            client.sendToClient(new Warning(successMessage.toString()));
            System.out.println("User logged in: " + username + " with phone: " + phoneNumber + 
                ", role: " + role + (branchName != null ? ", branch: " + branchName : ""));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleLogout(String msgString, ConnectionToClient client) {
        // Expected: "LOGOUT" or "LOGOUT;phoneNumber"
        try {
            String phoneNumber = null;
            if (msgString.contains(";")) {
                phoneNumber = msgString.split(";")[1].trim();
            } else {
                // Try to get phone from client connection info
                Object userObj = client.getInfo("user");
                if (userObj instanceof UserAccount) {
                    phoneNumber = ((UserAccount) userObj).getPhoneNumber();
                }
            }
            
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                // Fetch account and set to offline
                List<UserAccount> accounts = DataManager.fetchUserAccountsByPhoneNumber(phoneNumber);
                if (!accounts.isEmpty()) {
                    UserAccount account = accounts.get(0);
                    account.setOnline(false);
                    
                    org.hibernate.Session session = Database.getSessionFactoryInstance().openSession();
                    try {
                        session.beginTransaction();
                        session.merge(account);
                        session.getTransaction().commit();
                        System.out.println("Account logged out and set to offline: " + phoneNumber);
                        // No need to send success message - client already handles logout UI
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
                }
            }
            
            // Clear user info from connection
            client.setInfo("user", null);
        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
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

    public static void handleGetUserAccountInfo(String msgString, ConnectionToClient client) {
        // Expected: "GET_USER_ACCOUNT_INFO;phoneNumber"
        String[] parts = msgString.split(";");
        if (parts.length < 2) {
            try {
                client.sendToClient(new Warning("GET_USER_ACCOUNT_INFO_FAILURE: Invalid format"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String phoneNumber = parts[1].trim();
        List<UserAccount> accounts = DataManager.fetchUserAccountsByPhoneNumber(phoneNumber);
        
        if (accounts.isEmpty()) {
            try {
                client.sendToClient(new Warning("GET_USER_ACCOUNT_INFO_FAILURE: User account not found"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            UserAccount account = accounts.get(0);
            // Send account info as: "GET_USER_ACCOUNT_INFO_SUCCESS;name;phoneNumber;password"
            String response = String.format("GET_USER_ACCOUNT_INFO_SUCCESS;%s;%s;%s",
                account.getName(), account.getPhoneNumber(), account.getPassword());
            client.sendToClient(new Warning(response));
        } catch (IOException e) {
            e.printStackTrace();
            try {
                client.sendToClient(new Warning("GET_USER_ACCOUNT_INFO_FAILURE: Error sending account data"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void handleUpdateUserAccount(String msgString, ConnectionToClient client) {
        // Expected: "UPDATE_USER_ACCOUNT;oldPhoneNumber;newName;newPhoneNumber;newPassword"
        String[] parts = msgString.split(";");
        if (parts.length < 5) {
            try {
                client.sendToClient(new Warning("UPDATE_USER_ACCOUNT_FAILURE: Invalid format"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String oldPhoneNumber = parts[1].trim();
        String newName = parts[2].trim();
        String newPhoneNumber = parts[3].trim();
        String newPassword = parts[4].trim();

        // Validate required fields
        if (newName.isEmpty() || newPhoneNumber.isEmpty() || newPassword.isEmpty()) {
            try {
                client.sendToClient(new Warning("UPDATE_USER_ACCOUNT_FAILURE: All fields are required"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Find the user account
        List<UserAccount> accounts = DataManager.fetchUserAccountsByPhoneNumber(oldPhoneNumber);
        if (accounts.isEmpty()) {
            try {
                client.sendToClient(new Warning("UPDATE_USER_ACCOUNT_FAILURE: User account not found"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        UserAccount account = accounts.get(0);

        // Check if new phone number is already taken by another user
        if (!oldPhoneNumber.equals(newPhoneNumber)) {
            List<UserAccount> existingAccounts = DataManager.fetchUserAccountsByPhoneNumber(newPhoneNumber);
            if (!existingAccounts.isEmpty() && existingAccounts.get(0).getId() != account.getId()) {
                try {
                    client.sendToClient(new Warning("UPDATE_USER_ACCOUNT_FAILURE: Phone number already registered"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        // Update the account
        try {
            account.setName(newName);
            account.setPhoneNumber(newPhoneNumber);
            account.setPassword(newPassword);
            
            // Use Hibernate merge to update the entity
            org.hibernate.Session session = Database.getSessionFactoryInstance().openSession();
            try {
                session.beginTransaction();
                session.merge(account);
                session.getTransaction().commit();
                System.out.println("Updated user account: " + newName + " with phone: " + newPhoneNumber);
                client.sendToClient(new Warning("UPDATE_USER_ACCOUNT_SUCCESS;Account updated successfully"));
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
            e.printStackTrace();
            try {
                client.sendToClient(new Warning("UPDATE_USER_ACCOUNT_FAILURE: " + e.getMessage()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void handleForgotPasswordReset(String msgString, ConnectionToClient client) {
        // Expected: "FORGOT_PASSWORD_RESET;name;phoneNumber;newPassword"
        String[] parts = msgString.split(";");
        if (parts.length < 4) {
            try {
                client.sendToClient(new Warning("FORGOT_PASSWORD_RESET_FAILURE: Invalid format"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String name = parts[1].trim();
        String phoneNumber = parts[2].trim();
        String newPassword = parts[3].trim();

        // Validate required fields
        if (name.isEmpty() || phoneNumber.isEmpty() || newPassword.isEmpty()) {
            try {
                client.sendToClient(new Warning("FORGOT_PASSWORD_RESET_FAILURE: All fields are required"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (newPassword.length() < 3) {
            try {
                client.sendToClient(new Warning("FORGOT_PASSWORD_RESET_FAILURE: Password must be at least 3 characters long"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Find the user account by phone number
        List<UserAccount> accounts = DataManager.fetchUserAccountsByPhoneNumber(phoneNumber);
        if (accounts.isEmpty()) {
            try {
                client.sendToClient(new Warning("FORGOT_PASSWORD_RESET_FAILURE: No account found with this phone number"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        UserAccount account = accounts.get(0);

        // Verify that the name matches
        if (!account.getName().equalsIgnoreCase(name)) {
            try {
                client.sendToClient(new Warning("FORGOT_PASSWORD_RESET_FAILURE: Name does not match the account"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Update the password
        try {
            account.setPassword(newPassword);
            
            // Use Hibernate merge to update the entity
            org.hibernate.Session session = Database.getSessionFactoryInstance().openSession();
            try {
                session.beginTransaction();
                session.merge(account);
                session.getTransaction().commit();
                System.out.println("Password reset for user: " + name + " with phone: " + phoneNumber);
                client.sendToClient(new Warning("FORGOT_PASSWORD_RESET_SUCCESS;Password reset successfully"));
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
            e.printStackTrace();
            try {
                client.sendToClient(new Warning("FORGOT_PASSWORD_RESET_FAILURE: " + e.getMessage()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
