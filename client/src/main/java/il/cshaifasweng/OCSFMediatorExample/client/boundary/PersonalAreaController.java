package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.client.WarningEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.Reservation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox; // ✅ <-- This fixes your problem
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.geometry.Insets;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class PersonalAreaController {

    @FXML
    private ListView<Node> ordersListView;

    @FXML
    private ListView<HBox> reservationsListView;
    
    @FXML
    private Label statusLabel;

    @FXML
    private Button reportsButton;
    
    @FXML
    private Button tableManagementButton;
    
    @FXML
    private Button contactUsButton;
    
    @FXML
    private Hyperlink changeDetailsLink;
    
    @FXML
    private Label ordersLabel;
    
    @FXML
    private Label reservationsLabel;

    @FXML
    public void initialize() {
        // Register with EventBus to receive order updates
        EventBus.getDefault().register(this);
        
        // Ensure client is subscribed to receive broadcasts
        try {
            SimpleClient.getClient().sendToServer("add client");
            System.out.println("🔵 PersonalAreaController: Ensured client is subscribed");
        } catch (IOException e) {
            System.err.println("Failed to subscribe client: " + e.getMessage());
        }
        
        // Clear the lists
        ordersListView.getItems().clear();
        reservationsListView.getItems().clear();
        
        // Show loading message
        statusLabel.setText("Loading your data...");
        
        // Check user role
        String userRole = SimpleClient.getCurrentUserRole();
        boolean isWorker = "worker".equalsIgnoreCase(userRole);
        boolean isManager = "manager".equalsIgnoreCase(userRole);
        
        // Show reports button only for managers
        reportsButton.setVisible(isManager);
        
        // Show table management button for workers
        if (tableManagementButton != null) {
            tableManagementButton.setVisible(isWorker);
        }
        
        // Hide "Contact Us" button for workers (they don't make complaints)
        if (contactUsButton != null) {
            contactUsButton.setVisible(!isWorker);
        }
        
        // Hide "Change Personal Details" for workers (they don't need it)
        if (isWorker && changeDetailsLink != null) {
            changeDetailsLink.setVisible(false);
        }
        
        // Update labels for workers
        if (isWorker) {
            if (ordersLabel != null) {
                ordersLabel.setText("🧾 All Upcoming Orders");
            }
            if (reservationsLabel != null) {
                reservationsLabel.setText("📅 All Upcoming Reservations");
            }
        }
        
        // Request orders and reservations from server
        try {
            if (isWorker) {
                // Workers see all upcoming orders and reservations
                SimpleClient.getClient().getUpcomingOrders();
                SimpleClient.getClient().getUpcomingReservations();
                statusLabel.setText("Loading upcoming orders and reservations...");
            } else {
                // Regular clients see their own orders and reservations
                SimpleClient.getClient().getUserOrders();
                SimpleClient.getClient().getUserReservations();
            }
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading data: " + e.getMessage());
        }
    }
    
    @Subscribe
    public void onOrdersReceived(SimpleClient.OrdersReceivedEvent event) {
        // This runs on background thread, so use Platform.runLater
        System.out.println("🔵 PersonalAreaController: Received OrdersReceivedEvent with " + event.getOrders().size() + " orders");
        Platform.runLater(() -> {
            String userRole = SimpleClient.getCurrentUserRole();
            boolean isWorker = "worker".equalsIgnoreCase(userRole);
            String currentUserPhone = SimpleClient.getCurrentUserPhone();
            
            List<Order> orders = event.getOrders();
            
            // For regular clients: only process if all orders belong to them (direct request)
            // If any order doesn't belong to them, it's a broadcast meant for workers - ignore it
            if (!isWorker && currentUserPhone != null && !orders.isEmpty()) {
                boolean allOrdersBelongToUser = true;
                for (Order order : orders) {
                    if (order.getPhoneNumber() == null || !order.getPhoneNumber().equals(currentUserPhone)) {
                        allOrdersBelongToUser = false;
                        break;
                    }
                }
                if (!allOrdersBelongToUser) {
                    System.out.println("🔵 PersonalAreaController: Ignoring broadcast update (not a worker, and orders don't all belong to user)");
                    return; // Ignore broadcast updates for non-workers
                }
            }
            
            System.out.println("🔵 PersonalAreaController: Displaying " + orders.size() + " orders (isWorker: " + isWorker + ")");
            
            // Always update the display with new data (real-time updates)
            displayUserOrders(orders);
            if (orders.isEmpty()) {
                statusLabel.setText(isWorker ? "No upcoming orders" : "You have no orders");
            } else {
                statusLabel.setText(isWorker ? "Found " + orders.size() + " upcoming orders" : "Found " + orders.size() + " orders");
            }
        });
    }
    
    @Subscribe
    public void onReservationsReceived(SimpleClient.ReservationsReceivedEvent event) {
        // This runs on background thread, so use Platform.runLater
        Platform.runLater(() -> {
            String userRole = SimpleClient.getCurrentUserRole();
            boolean isWorker = "worker".equalsIgnoreCase(userRole);
            String currentUserPhone = SimpleClient.getCurrentUserPhone();
            
            List<Reservation> reservations = event.getReservations();
            
            // For regular clients: only process if all reservations belong to them (direct request)
            // If any reservation doesn't belong to them, it's a broadcast meant for workers - ignore it
            if (!isWorker && currentUserPhone != null && !reservations.isEmpty()) {
                boolean allReservationsBelongToUser = true;
                for (Reservation reservation : reservations) {
                    // Check if reservation phone number matches current user's phone
                    if (reservation.getPhoneNumber() == null || !reservation.getPhoneNumber().equals(currentUserPhone)) {
                        allReservationsBelongToUser = false;
                        break;
                    }
                }
                if (!allReservationsBelongToUser) {
                    System.out.println("🔵 PersonalAreaController: Ignoring broadcast update for reservations (not a worker)");
                    return; // Ignore broadcast updates for non-workers
                }
            }
            
            displayUserReservations(reservations);
            if (reservations.isEmpty()) {
                statusLabel.setText(statusLabel.getText() + (isWorker ? " and no upcoming reservations" : " and no reservations"));
            } else {
                statusLabel.setText(statusLabel.getText() + (isWorker ? " and " + reservations.size() + " upcoming reservations" : " and " + reservations.size() + " reservations"));
            }
        });
    }
    
    private void displayUserOrders(List<Order> orders) {
        System.out.println("🔵 PersonalAreaController: displayUserOrders called with " + orders.size() + " orders");
        ordersListView.getItems().clear();
        
        if (orders.isEmpty()) {
            System.out.println("🔵 PersonalAreaController: No orders to display");
            statusLabel.setText("You have no orders yet");
            return;
        }
        
        String userRole = SimpleClient.getCurrentUserRole();
        boolean isWorker = "worker".equalsIgnoreCase(userRole);
        
        for (Order order : orders) {
            System.out.println("🔵 PersonalAreaController: Processing order ID: " + order.getId() + ", Total: $" + order.getTotalCost());
            
            if (isWorker) {
                // For workers, show detailed order information
                addWorkerOrderItem(order);
            } else {
                // For regular customers, show simple format
                String orderTime = order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String details = String.format("Order #%d - $%.2f - %s", 
                    order.getId(), 
                    order.getTotalCost(),
                    orderTime);
                
                // Check if order is active (pending)
                boolean isActive = "Pending".equals(order.getStatus());
                
                addOrderItem(details, isActive);
            }
            System.out.println("🔵 PersonalAreaController: Added order item to list");
        }
        System.out.println("🔵 PersonalAreaController: ListView now has " + ordersListView.getItems().size() + " items");
    }

    private void displayUserReservations(List<Reservation> reservations) {
        reservationsListView.getItems().clear();
        
        if (reservations.isEmpty()) {
            return;
        }
        
        for (Reservation reservation : reservations) {
            // Format reservation details
            String reservationTime = reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            Branch branch = reservation.getBranch();
            String branchName = branch != null ? branch.getLocation() : "Unknown";
            
            String details = String.format("Reservation #%d - %s branch - %d guests - %s", 
                reservation.getId(), 
                branchName,
                reservation.getNumberOfGuests(),
                reservationTime);
            
            // Reservation is active if it's in the future
            boolean isActive = reservation.getReservationTime().isAfter(LocalDateTime.now());
            
            addReservationItem(details, isActive, reservation.getId());
        }
    }

    private void loadSampleReservations() {
        // This method is no longer used - we load real reservations
        reservationsListView.getItems().clear();
    }
    
    // Keep this for backward compatibility
    private void addReservationItem(String text, boolean isActive) {
        addReservationItem(text, isActive, -1);
    }

    private void addOrderItem(String text, boolean isActive) {
        System.out.println("🔵 PersonalAreaController: addOrderItem called with text: " + text);
        Label label = new Label((isActive ? "🟢 " : "⚪ ") + text);
        Button cancelButton = new Button("Cancel");
        cancelButton.setVisible(isActive);

        cancelButton.setOnAction(e -> {
            boolean confirmed = showCancelConfirmation();
            if (confirmed) {
                // Extract order ID from text (format: "Order #ID - ...")
                String idStr = text.substring(text.indexOf("#") + 1, text.indexOf(" - "));
                try {
                    int orderId = Integer.parseInt(idStr);
                    cancelOrder(orderId);
                    ordersListView.getItems().removeIf(row -> {
                        if (row instanceof HBox) {
                            HBox hbox = (HBox) row;
                            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Label) {
                                Label lbl = (Label) hbox.getChildren().get(0);
                                return lbl.getText().equals(label.getText());
                            }
                        }
                        return false;
                    });
                } catch (NumberFormatException ex) {
                    showError("Could not parse order ID");
                }
            }
        });

        HBox row = new HBox(10, label, cancelButton);
        ordersListView.getItems().add(row);
        System.out.println("🔵 PersonalAreaController: Added HBox to ListView. ListView size: " + ordersListView.getItems().size());
    }
    
    private void addWorkerOrderItem(Order order) {
        // Create expandable order display for workers
        String statusIcon = "Pending".equals(order.getStatus()) ? "🟡" : 
                           "Preparing".equals(order.getStatus()) ? "🟠" : 
                           "Ready".equals(order.getStatus()) ? "🟢" : "⚪";
        
        String orderTime = order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String headerText = String.format("%s Order #%d - $%.2f - %s", 
            statusIcon, order.getId(), order.getTotalCost(), orderTime);
        
        Label headerLabel = new Label(headerText);
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Create expandable details section
        VBox detailsBox = new VBox(5);
        detailsBox.setStyle("-fx-padding: 10; -fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 5;");
        detailsBox.setVisible(false);
        
        // Customer info
        Label customerLabel = new Label("👤 Customer: " + order.getCustomerName() + " (" + order.getPhoneNumber() + ")");
        customerLabel.setStyle("-fx-font-size: 12px;");
        detailsBox.getChildren().add(customerLabel);
        
        // Delivery/Pickup info
        String deliveryInfo;
        if ("PICKUP".equals(order.getDeliveryLocation())) {
            deliveryInfo = String.format("📍 Pickup: %s at %s", order.getDeliveryDate(), order.getDeliveryTime());
        } else {
            deliveryInfo = String.format("📍 Delivery: %s at %s to %s", 
                order.getDeliveryDate(), order.getDeliveryTime(), order.getDeliveryLocation());
        }
        Label deliveryLabel = new Label(deliveryInfo);
        deliveryLabel.setStyle("-fx-font-size: 12px;");
        detailsBox.getChildren().add(deliveryLabel);
        
        // Payment method
        Label paymentLabel = new Label("💳 Payment: " + order.getPaymentMethod());
        paymentLabel.setStyle("-fx-font-size: 12px;");
        detailsBox.getChildren().add(paymentLabel);
        
        // Order items and customizations with full meal details
        String orderItemsStr = order.getOrderItems();
        if (orderItemsStr != null && !orderItemsStr.isEmpty()) {
            Label itemsHeader = new Label("🍽️ Items:");
            itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 5 0 0 0;");
            detailsBox.getChildren().add(itemsHeader);
            
            // Parse order items: "itemName:quantity:note:ingredients:preferences:category:price|..."
            String[] items = orderItemsStr.split("\\|");
            for (String item : items) {
                if (item.isEmpty()) continue;
                String[] parts = item.split(":", 7);
                if (parts.length >= 2) {
                    // Unescape special characters
                    String itemName = parts[0].replace("::", ":").replace("||", "|");
                    int quantity = Integer.parseInt(parts[1]);
                    String note = parts.length > 2 ? parts[2].replace("::", ":").replace("||", "|") : "";
                    String ingredients = parts.length > 3 ? parts[3].replace("::", ":").replace("||", "|") : "";
                    String preferences = parts.length > 4 ? parts[4].replace("::", ":").replace("||", "|") : "";
                    String category = parts.length > 5 ? parts[5].replace("::", ":").replace("||", "|") : "";
                    double price = parts.length > 6 ? Double.parseDouble(parts[6]) : 0.0;
                    
                    // Create a container for each menu item with full details
                    VBox itemBox = new VBox(3);
                    itemBox.setStyle("-fx-padding: 8; -fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-radius: 3; -fx-spacing: 3;");
                    
                    // Item name and quantity
                    Label nameLabel = new Label(String.format("  📋 %s x%d - $%.2f", itemName, quantity, price * quantity));
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2c3e50;");
                    itemBox.getChildren().add(nameLabel);
                    
                    // Category
                    if (!category.isEmpty()) {
                        Label categoryLabel = new Label("     Category: " + category);
                        categoryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
                        itemBox.getChildren().add(categoryLabel);
                    }
                    
                    // Ingredients
                    if (!ingredients.isEmpty()) {
                        Label ingredientsLabel = new Label("     🥘 Ingredients: " + ingredients);
                        ingredientsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
                        ingredientsLabel.setWrapText(true);
                        itemBox.getChildren().add(ingredientsLabel);
                    }
                    
                    // Preferences
                    if (!preferences.isEmpty()) {
                        Label preferencesLabel = new Label("     ⚙️ Preferences: " + preferences);
                        preferencesLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
                        preferencesLabel.setWrapText(true);
                        itemBox.getChildren().add(preferencesLabel);
                    }
                    
                    // Customer note/customization
                    if (!note.isEmpty()) {
                        Label noteLabel = new Label("     📝 Customer Note: " + note);
                        noteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #d9534f; -fx-font-weight: bold;");
                        noteLabel.setWrapText(true);
                        itemBox.getChildren().add(noteLabel);
                    }
                    
                    detailsBox.getChildren().add(itemBox);
                }
            }
        } else {
            Label noItemsLabel = new Label("  (No items details available)");
            noItemsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
            detailsBox.getChildren().add(noItemsLabel);
        }
        
        // Toggle button to expand/collapse details
        Button toggleButton = new Button("▼ Details");
        toggleButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 5;");
        toggleButton.setOnAction(e -> {
            boolean isVisible = detailsBox.isVisible();
            detailsBox.setVisible(!isVisible);
            toggleButton.setText(isVisible ? "▼ Details" : "▲ Hide");
        });
        
        // Action buttons
        HBox buttonBox = new HBox(5);
        boolean canCancel = "Pending".equals(order.getStatus()) || "Preparing".equals(order.getStatus());
        boolean canMarkReady = "Pending".equals(order.getStatus()) || "Preparing".equals(order.getStatus());
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 11px;");
        cancelButton.setVisible(canCancel);
        cancelButton.setOnAction(e -> {
            boolean confirmed = showCancelConfirmation();
            if (confirmed) {
                cancelOrder(order.getId());
                // Remove this order from the list
                ordersListView.getItems().removeIf(row -> {
                    if (row instanceof VBox) {
                        VBox vbox = (VBox) row;
                        if (!vbox.getChildren().isEmpty() && vbox.getChildren().get(0) instanceof HBox) {
                            HBox hbox = (HBox) vbox.getChildren().get(0);
                            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Label) {
                                Label lbl = (Label) hbox.getChildren().get(0);
                                return lbl.getText().contains("Order #" + order.getId());
                            }
                        }
                    }
                    return false;
                });
            }
        });
        
        Button readyButton = new Button("Ready");
        readyButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 11px;");
        readyButton.setVisible(canMarkReady);
        readyButton.setOnAction(e -> {
            markOrderReady(order.getId());
            // Remove this order from the list immediately
            ordersListView.getItems().removeIf(row -> {
                if (row instanceof VBox) {
                    VBox vbox = (VBox) row;
                    if (!vbox.getChildren().isEmpty() && vbox.getChildren().get(0) instanceof HBox) {
                        HBox hbox = (HBox) vbox.getChildren().get(0);
                        if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Label) {
                            Label lbl = (Label) hbox.getChildren().get(0);
                            return lbl.getText().contains("Order #" + order.getId());
                        }
                    }
                }
                return false;
            });
        });
        
        buttonBox.getChildren().addAll(toggleButton, cancelButton, readyButton);
        
        // Main container
        HBox headerBox = new HBox(10, headerLabel, buttonBox);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        VBox orderContainer = new VBox(5, headerBox, detailsBox);
        orderContainer.setStyle("-fx-padding: 5; -fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5;");
        
        ordersListView.getItems().add(orderContainer);
    }
    
    private void cancelOrder(int orderId) {
        try {
            SimpleClient.getClient().sendToServer("CANCEL_ORDER;" + orderId);
            System.out.println("Sent cancel request for order: " + orderId);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to cancel order: " + e.getMessage());
        }
    }
    
    private void markOrderReady(int orderId) {
        try {
            SimpleClient.getClient().sendToServer("MARK_ORDER_READY;" + orderId);
            System.out.println("Sent mark ready request for order: " + orderId);
            statusLabel.setText("Order #" + orderId + " marked as ready!");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to mark order as ready: " + e.getMessage());
        }
    }

    private void addReservationItem(String text, boolean isActive, int reservationId) {
        Label label = new Label((isActive ? "🟢 " : "⚪ ") + text);
        Button cancelButton = new Button("Cancel");
        cancelButton.setVisible(isActive);

        cancelButton.setOnAction(e -> {
            boolean confirmed = showCancelConfirmation();
            if (confirmed) {
                reservationsListView.getItems().removeIf(row -> ((Label) row.getChildren().get(0)).getText().equals(label.getText()));
                System.out.println("Reservation canceled: " + text);
                try {
                    SimpleClient.getClient().sendToServer("CANCEL_RESERVATION;" + reservationId);
                    System.out.println("Sent cancel request for reservation: " + reservationId);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    showError("Failed to cancel reservation: " + ex.getMessage());
                }
            }
        });

        HBox row = new HBox(10, label, cancelButton);
        reservationsListView.getItems().add(row);
    }

    private boolean showCancelConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Confirmation");
        alert.setHeaderText("Are you sure you want to cancel?");
        alert.setContentText("Some cancellations may be subject to refund conditions.");

        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        // Send logout message to server to set isOnline flag to false
        try {
            String phoneNumber = SimpleClient.getCurrentUserPhone();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                SimpleClient.getClient().sendToServer("LOGOUT;" + phoneNumber);
                System.out.println("Sent logout message for: " + phoneNumber);
            } else {
                SimpleClient.getClient().sendToServer("LOGOUT");
                System.out.println("Sent logout message (no phone number)");
            }
            // Give server a moment to process logout
            Thread.sleep(100);
        } catch (Exception e) {
            System.err.println("Error sending logout message: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Clear client-side user data
        SimpleClient.setCurrentUserPhone(null);
        SimpleClient.setCurrentUserRole(null);
        SimpleClient.setBranchName(null);
        SimpleClient.setSelectedBranchId(0); // Reset to 0 (no branch selected)
        SimpleClient.setSelectedBranchName(null);
        
        // Unregister from EventBus
        EventBus.getDefault().unregister(this);
        
        try {
            App.setRoot("primary1");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleContactUs() {
        try {
            App.setRoot("contact_us"); // Updated to use our new contact_us page
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Failed to load contact page.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleViewReports() {
        try {
            App.setRoot("reports_view");
        } catch (IOException e) {
            showError("Error opening reports: " + e.getMessage());
        }
    }

    @FXML
    private void handleTableManagement() {
        try {
            String userRole = SimpleClient.getCurrentUserRole();
            
            // If user is a worker, automatically use their branch
            if ("worker".equalsIgnoreCase(userRole)) {
                String workerBranchName = SimpleClient.getBranchName();
                if (workerBranchName != null) {
                    // Map branch name to branch ID
                    Integer branchId = getBranchIdFromName(workerBranchName);
                    if (branchId != null) {
                        // Set the branch and go directly to table management
                        SimpleClient.setSelectedBranchId(branchId);
                        SimpleClient.setSelectedBranchName(workerBranchName);
                        App.setRoot("tables_map");
                        return;
                    }
                }
                showError("Unable to determine your branch. Please contact support.");
            } else {
                showError("Table management is only available for workers.");
            }
        } catch (IOException e) {
            showError("Error opening table management: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to get branch ID from branch name
     */
    private Integer getBranchIdFromName(String branchName) {
        if (branchName == null) {
            return null;
        }
        
        switch (branchName) {
            case "Tel-Aviv":
                return 1;
            case "Haifa":
                return 2;
            case "Jerusalem":
                return 3;
            case "Beer-Sheva":
                return 4;
            default:
                // Try to extract number from branch name
                String[] parts = branchName.split("\\D+");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        try {
                            return Integer.parseInt(part);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                return null;
        }
    }

    @FXML
    private void handleChangePersonalDetails() {
        // Request current user account info from server
        String currentPhone = SimpleClient.getCurrentUserPhone();
        if (currentPhone == null || currentPhone.isEmpty()) {
            showError("Unable to retrieve user information. Please log in again.");
            return;
        }

        try {
            SimpleClient.getClient().sendToServer("GET_USER_ACCOUNT_INFO;" + currentPhone);
            // The response will be handled by onWarningEvent
            statusLabel.setText("Loading your account information...");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to retrieve account information: " + e.getMessage());
        }
    }

    private void showChangeDetailsDialog(String currentName, String currentPhone, String currentPassword) {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Change Personal Details");
        dialog.setHeaderText("Update your personal information");

        // Create form fields
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField(currentName);

        Label phoneLabel = new Label("Username (Phone):");
        TextField phoneField = new TextField(currentPhone);

        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setText(currentPassword);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(phoneLabel, 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(passwordLabel, 0, 2);
        grid.add(passwordField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Enable/disable save button based on validation
        dialog.getDialogPane().lookupButton(saveButtonType).setDisable(false);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new String[]{
                    nameField.getText().trim(),
                    phoneField.getText().trim(),
                    passwordField.getText().trim()
                };
            }
            return null;
        });

        var result = dialog.showAndWait();
        if (result.isPresent() && result.get() != null) {
            String[] newDetails = result.get();
            String newName = newDetails[0];
            String newPhone = newDetails[1];
            String newPassword = newDetails[2];

            // Validate fields
            if (newName.isEmpty() || newPhone.isEmpty() || newPassword.isEmpty()) {
                showError("All fields are required. Please fill in all information.");
                return;
            }

            // Validate phone number format (same as order validation)
            if (newPhone.length() != 10) {
                showError("Phone number must be exactly 10 digits.");
                return;
            }
            if (!newPhone.startsWith("05")) {
                showError("Phone number must start with '05'.");
                return;
            }
            if (!newPhone.matches("\\d+")) {
                showError("Phone number can only contain numbers.");
                return;
            }

            // Send update request to server
            try {
                String updateMsg = String.format("UPDATE_USER_ACCOUNT;%s;%s;%s;%s",
                    currentPhone, newName, newPhone, newPassword);
                SimpleClient.getClient().sendToServer(updateMsg);
                statusLabel.setText("Updating your account information...");
            } catch (IOException e) {
                e.printStackTrace();
                showError("Failed to update account: " + e.getMessage());
            }
        }
    }

    @Subscribe
    public void onWarningEvent(WarningEvent event) {
        String message = event.getWarning().getMessage();
        
        if (message.startsWith("MARK_ORDER_READY_SUCCESS")) {
            Platform.runLater(() -> {
                String successMsg = message.substring(message.indexOf(":") + 1).trim();
                statusLabel.setText("✅ " + successMsg);
                // Refresh orders to show updated status
                String userRole = SimpleClient.getCurrentUserRole();
                boolean isWorker = "worker".equalsIgnoreCase(userRole);
                if (isWorker) {
                    try {
                        SimpleClient.getClient().getUpcomingOrders();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (message.startsWith("GET_USER_ACCOUNT_INFO_SUCCESS")) {
            Platform.runLater(() -> {
                // Parse: "GET_USER_ACCOUNT_INFO_SUCCESS;name;phoneNumber;password"
                String[] parts = message.split(";");
                if (parts.length >= 4) {
                    String name = parts[1];
                    String phone = parts[2];
                    String password = parts[3];
                    showChangeDetailsDialog(name, phone, password);
                } else {
                    showError("Failed to retrieve account information.");
                }
            });
        } else if (message.startsWith("GET_USER_ACCOUNT_INFO_FAILURE")) {
            Platform.runLater(() -> {
                String errorMsg = message.substring(message.indexOf(":") + 1).trim();
                showError("Failed to retrieve account information: " + errorMsg);
            });
        } else if (message.startsWith("UPDATE_USER_ACCOUNT_SUCCESS")) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Account Updated");
                alert.setContentText("Your personal details have been updated successfully!");
                alert.showAndWait();
                
                // Update SimpleClient with new phone if it changed
                // Note: We'd need to get the new phone from the update, but for now
                // the user will need to log in again with the new credentials
                statusLabel.setText("Account updated successfully! Please log in again with your new credentials.");
            });
        } else if (message.startsWith("UPDATE_USER_ACCOUNT_FAILURE")) {
            Platform.runLater(() -> {
                String errorMsg = message.substring(message.indexOf(":") + 1).trim();
                showError("Failed to update account: " + errorMsg);
            });
        }
    }
}
