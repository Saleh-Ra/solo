package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox; // ✅ <-- This fixes your problem
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
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
    public void initialize() {
        // Register with EventBus to receive order updates
        EventBus.getDefault().register(this);
        
        // Clear the lists
        ordersListView.getItems().clear();
        reservationsListView.getItems().clear();
        
        // Show loading message
        statusLabel.setText("Loading your orders...");
        
        // Request orders from server
        try {
            SimpleClient.getClient().getUserOrders();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading orders: " + e.getMessage());
        }
        
        // Load sample reservations for now
        loadSampleReservations();
    }
    
    @Subscribe
    public void onOrdersReceived(SimpleClient.OrdersReceivedEvent event) {
        // This runs on background thread, so use Platform.runLater
        Platform.runLater(() -> {
            List<Order> orders = event.getOrders();
            displayUserOrders(orders);
            statusLabel.setText("Found " + orders.size() + " orders");
        });
    }
    
    private void displayUserOrders(List<Order> orders) {
        ordersListView.getItems().clear();
        
        if (orders.isEmpty()) {
            statusLabel.setText("You have no orders yet");
            return;
        }
        
        for (Order order : orders) {
            // Format order details
            String orderTime = order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String details = String.format("Order #%d - $%.2f - %s", 
                order.getId(), 
                order.getTotalCost(),
                orderTime);
            
            // Check if order is active (pending)
            boolean isActive = "Pending".equals(order.getStatus());
            
            addOrderItem(details, isActive);
        }
    }

    private void loadSampleReservations() {
        reservationsListView.getItems().clear();

        addReservationItem("Reservation at Downtown - 2 guests, 2025-04-05 18:00", true);
        addReservationItem("Reservation at Beachside - 4 guests, 2025-04-01 20:00", false);
    }

    private void addOrderItem(String text, boolean isActive) {
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

    private void addReservationItem(String text, boolean isActive) {
        Label label = new Label((isActive ? "🟢 " : "⚪ ") + text);
        Button cancelButton = new Button("Cancel");
        cancelButton.setVisible(isActive);

        cancelButton.setOnAction(e -> {
            boolean confirmed = showCancelConfirmation();
            if (confirmed) {
                reservationsListView.getItems().removeIf(row -> ((Label) row.getChildren().get(0)).getText().equals(label.getText()));
                System.out.println("Reservation canceled: " + text);
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
            App.setRoot("contact_options"); // this FXML will be created next
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Failed to load contact options.");
            alert.showAndWait();
        }
    }
}
