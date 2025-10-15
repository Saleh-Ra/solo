package il.cshaifasweng.OCSFMediatorExample.server.Handlers;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.server.SimpleServer;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.Database;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;
import org.hibernate.Hibernate;
import org.hibernate.Session;

import java.util.List;
import java.util.ArrayList;

/* to be added
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
 */
public class OrderHandler {
    //this method here will be called only after the customer finds a good time, otherwise this should not be called
    //the reason to that is that this method will only add to the database, it will not check any details
    public static void handleCreateOrder(String msgString, ConnectionToClient client) {
        System.out.println("Received order request: " + msgString);
        LocalDateTime now = LocalDateTime.now();
        
        // Format: CREATE_ORDER;customerName;phone;deliveryDate;deliveryTime;deliveryLocation;paymentMethod;totalCost;branchId
        String[] parts = msgString.split(";");
        if (parts.length < 9) {
            System.out.println("Invalid order format: " + msgString);
            SimpleServer.sendFailureResponse(client, "CREATE_ORDER_FAILURE", "Invalid format. Expected 9 parts, got " + parts.length);
            return;
        }

        try {
            String customerName = parts[1];
            String phone = parts[2];
            String deliveryDate = parts[3];
            String deliveryTime = parts[4]; 
            String deliveryLocation = parts[5];
            String paymentMethod = parts[6];
            double totalCost = Double.parseDouble(parts[7]);
            int branchId = Integer.parseInt(parts[8]);
            
            // Validate branch ID
            if (branchId < 1 || branchId > 4) {
                System.out.println("Invalid branch ID: " + branchId);
                SimpleServer.sendFailureResponse(client, "CREATE_ORDER_FAILURE", "Invalid branch ID: " + branchId);
                return;
            }
            
            System.out.println("Creating order for customer: " + customerName);
            System.out.println("Phone: " + phone);
            System.out.println("Delivery: " + deliveryDate + " at " + deliveryTime + " to " + deliveryLocation);
            System.out.println("Payment method: " + paymentMethod);
            System.out.println("Total cost: $" + totalCost);
            System.out.println("Branch ID: " + branchId);
            
            // Get or create user account (same behavior as reservations)
            List<UserAccount> existingAccounts = DataManager.fetchUserAccountsByPhoneNumber(phone);
            UserAccount userAccount;
            boolean isNewAccount = false;
            
            if (existingAccounts.isEmpty()) {
                // Create new account for first-time customer
                System.out.println("No user account found with phone: " + phone + ". Creating new account.");
                String username = phone;
                String password = customerName.toLowerCase() + "123";
                userAccount = new UserAccount(customerName, phone, "client", password);
                DataManager.add(userAccount);
                isNewAccount = true;
                System.out.println("Created new user account with ID: " + userAccount.getId() + 
                    ", Username: " + username + ", Password: " + password);
            } else {
                // Phone number exists - verify the name matches
                userAccount = existingAccounts.get(0);
                if (!userAccount.getName().equalsIgnoreCase(customerName.trim())) {
                    System.out.println("❌ Phone number " + phone + " already registered to different customer: " + 
                        userAccount.getName() + " (tried to use with name: " + customerName + ")");
                    SimpleServer.sendFailureResponse(client, "CREATE_ORDER_FAILURE", 
                        "This phone number is already registered to another customer. Please use your registered name or a different phone number.");
                    return;
                }
                System.out.println("Found existing user account: " + userAccount.getName() + " with ID: " + userAccount.getId());
            }
            
            // Get or create Client entity for this UserAccount (same as reservations)
            List<Client> clients = DataManager.fetchClientsByPhoneNumber(phone);
            Client customer;
            
            if (clients.isEmpty()) {
                System.out.println("No Client entity found. Creating Client for UserAccount ID: " + userAccount.getId());
                customer = new Client(userAccount.getName(), userAccount);
                if (customer.getOrders() == null) {
                    customer.setOrders(new ArrayList<>());
                }
                DataManager.add(customer);
                System.out.println("Created Client entity with ID: " + customer.getId());
            } else {
                customer = clients.get(0);
                if (customer.getOrders() == null) {
                    customer.setOrders(new ArrayList<>());
                }
                System.out.println("Found existing Client entity with ID: " + customer.getId());
            }
            
            // Create order object with the selected branch ID and link to Client
            Order order = new Order(branchId, totalCost, now, customer);
            
            // Set additional details
            order.setStatus("Pending");
            order.setCustomerName(customerName);
            order.setPhoneNumber(phone);
            order.setDeliveryDate(deliveryDate);
            order.setDeliveryTime(deliveryTime);
            order.setDeliveryLocation(deliveryLocation);
            order.setPaymentMethod(paymentMethod);
            
            // Save both order and update client in a single transaction
            Session saveSession = Database.getSessionFactoryInstance().openSession();
            try {
                saveSession.beginTransaction();
                
                // First, reload the client to get fresh data
                Client freshClient = saveSession.get(Client.class, customer.getId());
                
                // Save the order
                saveSession.save(order);
                System.out.println("Order saved with ID: " + order.getId());
                
                // Add order to client's list in the same session
                if (freshClient.getOrders() == null) {
                    System.out.println("Client orders list was null, initializing...");
                    freshClient.setOrders(new ArrayList<>());
                }
                freshClient.getOrders().add(order);
                System.out.println("Added order to client's list. Client now has " + freshClient.getOrders().size() + " orders");
                
                saveSession.getTransaction().commit();
                System.out.println("Order and client updated in the same transaction");
            } catch (Exception e) {
                System.err.println("Error saving order: " + e.getMessage());
                e.printStackTrace();
                if (saveSession.getTransaction() != null && saveSession.getTransaction().isActive()) {
                    saveSession.getTransaction().rollback();
                }
                SimpleServer.sendFailureResponse(client, "ORDER_FAILURE", "Error saving order: " + e.getMessage());
                return;
            } finally {
                saveSession.close();
            }
            
            System.out.println("Order linked to branch ID: " + branchId);
            
            //now change the client's order list
            SimpleServer.sendUpdatedOrdersToClient(phone, client);
            
            // Send success response with account credentials if new account was created
            String successMessage = "Order created successfully with ID: " + order.getId();
            if (isNewAccount) {
                String username = phone;
                String password = customerName.toLowerCase() + "123";
                successMessage = String.format("Order created successfully with ID: %d! Your account has been created. Username: %s, Password: %s", 
                    order.getId(), username, password);
            }
            SimpleServer.sendSuccessResponse(client, "CREATE_ORDER_SUCCESS", successMessage);
        } catch (NumberFormatException e) {
            System.out.println("Failed to parse total cost: " + e.getMessage());
            SimpleServer.sendFailureResponse(client, "CREATE_ORDER_FAILURE", "Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Failed to create order: " + e.getMessage());
            e.printStackTrace();
            SimpleServer.sendFailureResponse(client, "CREATE_ORDER_FAILURE", "Failed to create order: " + e.getMessage());
        }
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
        if (minutesBefore >= 180) {
            refundPercentage = 1.0; // Full refund
        } else if (minutesBefore >= 60) {
            refundPercentage = 0.5; // Half refund
        } else {
            refundPercentage = 0.0; // No refund
        }

        System.out.println("Refund for client: " + (refundPercentage * 100) + "%");
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

    public static void handleGetOrdersByPhoneNumber(String msgString, ConnectionToClient client) {
        // Format: GET_USER_ORDERS;phoneNumber
        String[] parts = msgString.split(";");
        if (parts.length < 2) {
            SimpleServer.sendFailureResponse(client, "GET_ORDERS_FAILURE", "Invalid format");
            return;
        }

        try (Session session = Database.getSessionFactoryInstance().openSession()) {
            String phoneNumber = parts[1];
            System.out.println("Getting orders for phone: " + phoneNumber);
            
            // Find the client by phone number in the same session
            String hql = "FROM Client c WHERE c.account.phoneNumber = :phoneNumber";
            List<Client> clients = session.createQuery(hql, Client.class)
                    .setParameter("phoneNumber", phoneNumber)
                    .getResultList();
                    
            if (clients.isEmpty()) {
                System.out.println("No client found with phone: " + phoneNumber);
                // Send empty list
                client.sendToClient(new ArrayList<Order>());
                return;
            }
            
            Client clientEntity = clients.get(0);
            System.out.println("Found client: " + clientEntity.getName() + " with ID: " + clientEntity.getId());
            
            // Initialize the lazy-loaded orders collection while session is open
            Hibernate.initialize(clientEntity.getOrders());
            
            // Get orders for this client
            List<Order> orders = clientEntity.getOrders();
            if (orders == null) {
                System.out.println("WARNING: Client's orders list is null!");
                orders = new ArrayList<>();
            }
            
            System.out.println("Found " + orders.size() + " orders for client");
            for (Order o : orders) {
                System.out.println("  - Order ID: " + o.getId() + ", Total: $" + o.getTotalCost() + ", Status: " + o.getStatus());
            }
            
            // Send orders to client
            client.sendToClient(orders);
            
        } catch (Exception e) {
            System.err.println("Error getting user orders: " + e.getMessage());
            e.printStackTrace();
            SimpleServer.sendFailureResponse(client, "GET_ORDERS_FAILURE", "Server error: " + e.getMessage());
        }
    }
}
