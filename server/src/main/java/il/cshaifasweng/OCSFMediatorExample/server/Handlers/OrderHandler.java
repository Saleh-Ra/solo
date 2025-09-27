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
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;

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
            
            // Create order object with the selected branch ID
            Order order = new Order(branchId, totalCost, now, null);
            
            // Set additional details
            order.setStatus("Pending");
            order.setCustomerName(customerName);
            order.setPhoneNumber(phone);
            order.setDeliveryDate(deliveryDate);
            order.setDeliveryTime(deliveryTime);
            order.setDeliveryLocation(deliveryLocation);
            order.setPaymentMethod(paymentMethod);
            
            // Save to database
            System.out.println("Saving order to database...");
            DataManager.add(order);
            System.out.println("Order saved with ID: " + order.getId());
            
            // Note: Branch monthly orders are transient fields, so we don't need to update them here
            // The order is already linked to the branch via branchId field
            System.out.println("Order linked to branch ID: " + branchId);
            
            //now change the client's order list
            SimpleServer.sendUpdatedOrdersToClient(phone, client);
            // Send success response
            SimpleServer.sendSuccessResponse(client, "CREATE_ORDER_SUCCESS", "Order created successfully with ID: " + order.getId());
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
        
        String phoneNumber = parts[1];
        System.out.println("Fetching orders for phone number: " + phoneNumber);
        
        try {
            // Get all orders
            List<Order> allOrders = DataManager.fetchAll(Order.class);
            
            // Check if this is a manager by looking at their role
            Object clientInfo = client.getInfo("user");
            List<Order> userOrders = new ArrayList<>();
            
            if (clientInfo != null && clientInfo instanceof UserAccount) {
                UserAccount user = (UserAccount) clientInfo;
                if ("manager".equalsIgnoreCase(user.getRole()) && user.getBranchId() != null) {
                    // Manager: show orders for their branch
                    int branchId = user.getBranchId();
                    System.out.println("Manager requesting orders for branch ID: " + branchId);
                    
                    for (Order order : allOrders) {
                        if (order.getBranchId() == branchId) {
                            userOrders.add(order);
                            System.out.println("Found branch order: " + order.getId() + " for branch: " + branchId);
                        }
                    }
                } else {
                    // Regular user: show their own orders
                    for (Order order : allOrders) {
                        if (phoneNumber.equals(order.getPhoneNumber())) {
                            userOrders.add(order);
                            System.out.println("Found user order: " + order.getId() + " for phone: " + phoneNumber);
                        }
                    }
                }
            } else {
                // Fallback: show user's own orders
                for (Order order : allOrders) {
                    if (phoneNumber.equals(order.getPhoneNumber())) {
                        userOrders.add(order);
                        System.out.println("Found user order: " + order.getId() + " for phone: " + phoneNumber);
                    }
                }
            }
            
            // Send orders to client
            client.sendToClient(userOrders);
            System.out.println("Sent " + userOrders.size() + " orders to client");
        } catch (Exception e) {
            System.err.println("Error fetching orders: " + e.getMessage());
            e.printStackTrace();
            try {
                SimpleServer.sendFailureResponse(client, "GET_ORDERS_FAILURE", "Error fetching orders");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
