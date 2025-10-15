package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Cart;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.control.PasswordField;

public class OrderController {

    @FXML private TextField nameField;
    @FXML private TextField idField; // This field is used for phone number input
    @FXML private TextField addressField;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private Button confirmButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private final Cart cart = SimpleClient.getCart();

    @FXML
    public void initialize() {
        paymentMethodComboBox.getItems().addAll("Credit Card", "Cash");
        
        // Use the current user's phone number if available
        if (SimpleClient.getCurrentUserPhone() != null && !SimpleClient.getCurrentUserPhone().isEmpty()) {
            idField.setText(SimpleClient.getCurrentUserPhone());
        }
    }

    @FXML
    private void handleConfirm() {
        String name = nameField.getText().trim();
        String phone = idField.getText().trim();
        String address = addressField.getText().trim();
        LocalDate date = datePicker.getValue();
        String time = timeField.getText().trim();
        String method = paymentMethodComboBox.getValue();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || date == null || time.isEmpty() || method == null) {
            statusLabel.setText("❌ Please fill out all fields.");
            return;
        }
        
        // Validate phone number
        if (phone.length() < 10) {
            statusLabel.setText("❌ Please enter a valid phone number (at least 10 digits).");
            return;
        }

        try {
            LocalTime.parse(time);
        } catch (Exception e) {
            statusLabel.setText("❌ Invalid time format. Use HH:mm.");
            return;
        }

        // ✅ Credit card form popup
        if (method.equals("Credit Card")) {
            Dialog<String[]> cardDialog = new Dialog<>();
            cardDialog.setTitle("Enter Credit Card Info");
            cardDialog.setHeaderText("Please provide your credit card details");

            Label numberLabel = new Label("Card Number:");
            TextField numberField = new TextField();

            Label expLabel = new Label("Expiration (MM/YY):");
            TextField expField = new TextField();

            Label cvvLabel = new Label("CVV:");
            PasswordField cvvField = new PasswordField();

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            grid.add(numberLabel, 0, 0);
            grid.add(numberField, 1, 0);
            grid.add(expLabel, 0, 1);
            grid.add(expField, 1, 1);
            grid.add(cvvLabel, 0, 2);
            grid.add(cvvField, 1, 2);

            cardDialog.getDialogPane().setContent(grid);
            ButtonType okButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
            cardDialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

            cardDialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButtonType) {
                    return new String[]{numberField.getText(), expField.getText(), cvvField.getText()};
                }
                return null;
            });

            var result = cardDialog.showAndWait();
            if (result.isEmpty() || result.get()[0].isEmpty() || result.get()[1].isEmpty() || result.get()[2].isEmpty()) {
                statusLabel.setText("❌ Incomplete credit card details.");
                return;
            }

            System.out.println("💳 Card Info: " + result.get()[0] + " | Exp: " + result.get()[1] + " | CVV: " + result.get()[2]);
        }

        System.out.println("Customer: " + name + " | Phone: " + phone);
        System.out.println("Delivery: " + date + " at " + time + " to " + address);
        System.out.println("Payment: " + method + " | Total: $" + cart.calculateTotal());

        // Send order to server
        try {
            // Get the selected branch ID from SimpleClient
            int selectedBranchId = SimpleClient.getSelectedBranchId();
            System.out.println("Creating order for branch ID: " + selectedBranchId);
            
            // Format the order message matching server expectations
            // Format: CREATE_ORDER;customerName;phone;deliveryDate;deliveryTime;deliveryLocation;paymentMethod;totalCost;branchId
            String orderMsg = String.format("CREATE_ORDER;%s;%s;%s;%s;%s;%s;%.2f;%d", 
                name, phone, date.toString(), time, address, method, cart.calculateTotal(), selectedBranchId);
            
            // Send to server
            SimpleClient.getClient().sendToServer(orderMsg);
            System.out.println("Order sent to server: " + orderMsg);
            
            // Clear cart and show success message
            cart.clearCart();
            
            // Check if this response includes account credentials (new account created)
            // The server sends a message like: "Order created successfully with ID: X! Your account has been created. Username: Y, Password: Z"
            // We'll just show the success message as-is
            statusLabel.setText("🎉 Order placed successfully!");
            
            // Show alert with full details
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Order Placed Successfully");
            alert.setHeaderText("Your order has been placed!");
            alert.setContentText("Our kitchen has started preparing it. Expect delivery at your selected time.\n\nThank you for choosing us!");
            alert.showAndWait();

            try {
                App.setRoot("primary1");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to send order: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("user_menu");
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to go back to menu.");
        }
    }
}
