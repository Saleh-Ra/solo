package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.Reservation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox; // ✅ <-- This fixes your problem
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class PersonalAreaController {

    @FXML
    private ListView<HBox> ordersListView;

    @FXML
    private ListView<HBox> reservationsListView;
    
    @FXML
    private Label statusLabel;

    @FXML
    private Button reportsButton;

    @FXML
    public void initialize() {
        // Register with EventBus to receive order updates
        EventBus.getDefault().register(this);
        
        // Clear the lists
        ordersListView.getItems().clear();
        reservationsListView.getItems().clear();
        
        // Show loading message
        statusLabel.setText("Loading your data...");
        
        // Show reports button only for managers
        String userRole = SimpleClient.getCurrentUserRole();
        reportsButton.setVisible("manager".equalsIgnoreCase(userRole));
        
        // Request orders and reservations from server
        try {
            SimpleClient.getClient().getUserOrders();
            SimpleClient.getClient().getUserReservations();
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
            List<Order> orders = event.getOrders();
            System.out.println("🔵 PersonalAreaController: Displaying " + orders.size() + " orders");
            
            // Only clear and update if we actually have orders or this is the first load
            if (!orders.isEmpty() || ordersListView.getItems().isEmpty()) {
                displayUserOrders(orders);
                if (orders.isEmpty()) {
                    statusLabel.setText("You have no orders");
                } else {
                    statusLabel.setText("Found " + orders.size() + " orders");
                }
            } else {
                System.out.println("🔵 PersonalAreaController: Ignoring empty orders event (already have orders displayed)");
            }
        });
    }
    
    @Subscribe
    public void onReservationsReceived(SimpleClient.ReservationsReceivedEvent event) {
        // This runs on background thread, so use Platform.runLater
        Platform.runLater(() -> {
            List<Reservation> reservations = event.getReservations();
            displayUserReservations(reservations);
            if (reservations.isEmpty()) {
                statusLabel.setText(statusLabel.getText() + " and no reservations");
            } else {
                statusLabel.setText(statusLabel.getText() + " and " + reservations.size() + " reservations");
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
        
        for (Order order : orders) {
            System.out.println("🔵 PersonalAreaController: Processing order ID: " + order.getId() + ", Total: $" + order.getTotalCost());
            // Format order details
            String orderTime = order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String details = String.format("Order #%d - $%.2f - %s", 
                order.getId(), 
                order.getTotalCost(),
                orderTime);
            
            // Check if order is active (pending)
            boolean isActive = "Pending".equals(order.getStatus());
            
            addOrderItem(details, isActive);
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
                    ordersListView.getItems().removeIf(row -> ((Label) row.getChildren().get(0)).getText().equals(label.getText()));
                } catch (NumberFormatException ex) {
                    showError("Could not parse order ID");
                }
            }
        });

        HBox row = new HBox(10, label, cancelButton);
        ordersListView.getItems().add(row);
        System.out.println("🔵 PersonalAreaController: Added HBox to ListView. ListView size: " + ordersListView.getItems().size());
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
}
