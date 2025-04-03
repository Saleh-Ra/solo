package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox; // ✅ <-- This fixes your problem

import java.io.IOException;


public class PersonalAreaController {

    @FXML
    private ListView<HBox> ordersListView;

    @FXML
    private ListView<HBox> reservationsListView;

    @FXML
    public void initialize() {
        loadSampleOrders();
        loadSampleReservations();
    }

    private void loadSampleOrders() {
        ordersListView.getItems().clear();

        addOrderItem("Order #1234 - 2x Burger, 1x Fries", true);  // active
        addOrderItem("Order #1235 - 1x Pizza, 2x Coke", false);   // past
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
                ordersListView.getItems().removeIf(row -> ((Label) row.getChildren().get(0)).getText().equals(label.getText()));
                System.out.println("Order canceled: " + text);
            }
        });

        HBox row = new HBox(10, label, cancelButton);
        ordersListView.getItems().add(row);
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

    @FXML
    private void handleLogout() {
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
