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
        
        // Format: CREATE_ORDER;customerName;phone;deliveryDate;deliveryTime;deliveryLocation;paymentMethod;totalCost;branchId;orderType;orderItems
        String[] parts = msgString.split(";");
        if (parts.length < 9) {
            System.out.println("Invalid order format: " + msgString);
            SimpleServer.sendFailureResponse(client, "CREATE_ORDER_FAILURE", "Invalid format. Expected at least 9 parts, got " + parts.length);
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
            String orderType = parts.length > 9 ? parts[9] : "delivery"; // Default to delivery for backward compatibility
            String orderItems = parts.length > 10 ? parts[10] : ""; // Order items and notes
            boolean isPickup = "pickup".equalsIgnoreCase(orderType);
            
            // Validate branch ID
            if (branchId < 1 || branchId > 4) {
                System.out.println("Invalid branch ID: " + branchId);
                SimpleServer.sendFailureResponse(client, "CREATE_ORDER_FAILURE", "Invalid branch ID: " + branchId);
                return;
            }
            
            System.out.println("Creating " + (isPickup ? "PICKUP" : "DELIVERY") + " order for customer: " + customerName);
            System.out.println("Phone: " + phone);
            if (isPickup) {
                System.out.println("Pickup: " + deliveryDate + " at " + deliveryTime);
            } else {
                System.out.println("Delivery: " + deliveryDate + " at " + deliveryTime + " to " + deliveryLocation);
            }
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
            // For pickup orders, set location to "PICKUP" or leave as is
            if (isPickup) {
                order.setDeliveryLocation("PICKUP");
            } else {
                order.setDeliveryLocation(deliveryLocation);
            }
            order.setPaymentMethod(paymentMethod);
            order.setOrderItems(orderItems); // Store order items and customizations
            
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
            
            // Broadcast updated orders to all workers (real-time update)
            SimpleServer.sendUpdatedOrdersToAllWorkers();
            
            // Send success response with account credentials if new account was created
            String orderTypeText = isPickup ? "Pickup" : "Delivery";
            String successMessage = String.format("%s order created successfully with ID: %d", orderTypeText, order.getId());
            if (isNewAccount) {
                String username = phone;
                String password = customerName.toLowerCase() + "123";
                successMessage = String.format("%s order created successfully with ID: %d! Your account has been created. Username: %s, Password: %s", 
                    orderTypeText, order.getId(), username, password);
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
        String[] parts = msgString.split(";");
        if (parts.length < 2) {
            SimpleServer.sendFailureResponse(client, "CANCEL_ORDER_FAILURE", "Invalid format");
            return;
        }

        Session session = null;
        try {
            int orderId = Integer.parseInt(parts[1]);
            System.out.println("🗑️ Processing cancellation for order ID: " + orderId);

            // Open a single session for all operations
            session = Database.getSessionFactoryInstance().openSession();
            session.beginTransaction();

            // Load the order in this session
            Order order = session.get(Order.class, orderId);
            if (order == null) {
                session.getTransaction().rollback();
                SimpleServer.sendFailureResponse(client, "CANCEL_ORDER_FAILURE", "Order not found");
                return;
            }

            // Calculate refund percentage
            LocalDateTime orderTime = order.getOrderTime();
            LocalDateTime now = LocalDateTime.now();
            Duration diff = Duration.between(orderTime, now);
            long minutesAfter = diff.toMinutes();

            // Calculate how many minutes BEFORE the order time (negative if order is in the past)
            // For future orders: minutesBefore = -minutesAfter
            // For past orders: minutesBefore is negative (no refund)
            double refundPercentage;
            if (minutesAfter < 0) {
                // Order is in the future
                long minutesBefore = -minutesAfter;
                if (minutesBefore >= 180) {
                    refundPercentage = 1.0; // Full refund
                } else if (minutesBefore >= 60) {
                    refundPercentage = 0.5; // Half refund
                } else {
                    refundPercentage = 0.0; // No refund
                }
            } else {
                // Order is in the past - no refund
                refundPercentage = 0.0;
            }

            System.out.println("💰 Refund percentage: " + (refundPercentage * 100) + "%");

            // Remove the order from the client's list (if client exists)
            Client customer = order.getClient();
            if (customer != null) {
                // Reload client in this session to ensure it's attached
                customer = session.get(Client.class, customer.getId());
                if (customer != null && customer.getOrders() != null) {
                    customer.getOrders().remove(order);
                    System.out.println("✅ Removed order from client's list");
                }
            }

            // Delete associated Payment if it exists (cascade should handle this, but being explicit)
            if (order.getPayment() != null) {
                session.delete(order.getPayment());
                System.out.println("✅ Deleted associated payment");
            }

            // Delete associated Delivery if it exists (cascade should handle this, but being explicit)
            if (order.getDelivery() != null) {
                session.delete(order.getDelivery());
                System.out.println("✅ Deleted associated delivery");
            }

            // Delete the order (it's attached to the session, so this will work)
            session.delete(order);
            System.out.println("✅ Deleted order from database");

            // Commit all changes in one transaction
            session.getTransaction().commit();
            System.out.println("✅ Transaction committed successfully");

            SimpleServer.sendSuccessResponse(client, "CANCEL_ORDER_SUCCESS",
                    "Order cancelled. Refund: " + (int)(refundPercentage * 100) + "%");
            System.out.println("🎉 Order cancelled successfully: " + orderId);

        } catch (NumberFormatException e) {
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            SimpleServer.sendFailureResponse(client, "CANCEL_ORDER_FAILURE", "Invalid order ID");
        } catch (Exception e) {
            System.err.println("❌ Error cancelling order: " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                System.err.println("❌ Transaction rolled back due to error");
            }
            try {
                SimpleServer.sendFailureResponse(client, "CANCEL_ORDER_FAILURE", "Error cancelling order: " + e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
                System.out.println("✅ Session closed");
            }
        }
    }

    public static void handleMarkOrderReady(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        if (parts.length < 2) {
            SimpleServer.sendFailureResponse(client, "MARK_ORDER_READY_FAILURE", "Invalid format");
            return;
        }

        Session session = null;
        try {
            int orderId = Integer.parseInt(parts[1]);
            System.out.println("✅ Processing mark ready for order ID: " + orderId);

            // Open a session for the operation
            session = Database.getSessionFactoryInstance().openSession();
            session.beginTransaction();

            // Load the order in this session
            Order order = session.get(Order.class, orderId);
            if (order == null) {
                session.getTransaction().rollback();
                SimpleServer.sendFailureResponse(client, "MARK_ORDER_READY_FAILURE", "Order not found");
                return;
            }

            // Update order status to "Ready"
            order.setStatus("Ready");
            session.merge(order);
            
            // Commit the transaction
            session.getTransaction().commit();
            System.out.println("✅ Order marked as ready: " + orderId);

            // Broadcast updated orders to all workers (real-time update)
            SimpleServer.sendUpdatedOrdersToAllWorkers();

            SimpleServer.sendSuccessResponse(client, "MARK_ORDER_READY_SUCCESS",
                    "Order #" + orderId + " marked as ready!");
            System.out.println("🎉 Order marked as ready successfully: " + orderId);

        } catch (NumberFormatException e) {
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            SimpleServer.sendFailureResponse(client, "MARK_ORDER_READY_FAILURE", "Invalid order ID");
        } catch (Exception e) {
            System.err.println("❌ Error marking order as ready: " + e.getMessage());
            e.printStackTrace();
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                System.err.println("❌ Transaction rolled back due to error");
            }
            try {
                SimpleServer.sendFailureResponse(client, "MARK_ORDER_READY_FAILURE", "Error marking order as ready: " + e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
                System.out.println("✅ Session closed");
            }
        }
    }

    public static void handleGetAllOrders(ConnectionToClient client) {
        try {
            List<Order> orders = DataManager.fetchAll(Order.class);
            client.sendToClient(orders);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get all upcoming orders (for workers to prepare)
     * Returns orders with status "Pending" or "Preparing" that are scheduled for today or future
     */
    public static void handleGetUpcomingOrders(ConnectionToClient client) {
        try (Session session = Database.getSessionFactoryInstance().openSession()) {
            // Get current date/time
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDate today = now.toLocalDate();
            
            // Query for orders with status Pending or Preparing
            // Note: deliveryDate is stored as String, so we'll filter in Java
            String hql = "FROM Order o WHERE (o.status = 'Pending' OR o.status = 'Preparing') " +
                        "ORDER BY o.deliveryDate ASC, o.deliveryTime ASC";
            
            List<Order> allOrders = session.createQuery(hql, Order.class).getResultList();
            
            // Filter orders by date: deliveryDate is stored as String in ISO format (YYYY-MM-DD)
            List<Order> upcomingOrders = new ArrayList<>();
            for (Order order : allOrders) {
                String deliveryDateStr = order.getDeliveryDate();
                if (deliveryDateStr != null && !deliveryDateStr.isEmpty()) {
                    try {
                        // Parse the date string (format: YYYY-MM-DD from LocalDate.toString())
                        java.time.LocalDate deliveryDate = java.time.LocalDate.parse(deliveryDateStr);
                        // Include orders for today or future
                        if (!deliveryDate.isBefore(today)) {
                            upcomingOrders.add(order);
                        }
                    } catch (java.time.format.DateTimeParseException e) {
                        // If date format is unexpected, log and skip
                        System.err.println("Warning: Could not parse deliveryDate '" + deliveryDateStr + "' for order ID " + order.getId());
                    }
                }
            }
            
            System.out.println("Found " + upcomingOrders.size() + " upcoming orders for worker (out of " + allOrders.size() + " pending/preparing orders)");
            client.sendToClient(upcomingOrders);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                client.sendToClient(new ArrayList<Order>());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                client.sendToClient(new ArrayList<Order>());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
