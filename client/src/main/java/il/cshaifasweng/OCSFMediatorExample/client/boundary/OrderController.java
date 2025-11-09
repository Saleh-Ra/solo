package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.client.WarningEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Cart;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.control.PasswordField;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
        // Register with EventBus to receive server responses (only if not already registered)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        
        paymentMethodComboBox.getItems().addAll("Credit Card", "Cash");
        
        // Use the current user's phone number if available
        if (SimpleClient.getCurrentUserPhone() != null && !SimpleClient.getCurrentUserPhone().isEmpty()) {
            idField.setText(SimpleClient.getCurrentUserPhone());
        }
    }
    
    // Handle server responses
    @Subscribe
    public void onWarningEvent(WarningEvent event) {
        String message = event.getWarning().getMessage();
        System.out.println("OrderController: Received warning event: " + message);
        
        if (message.startsWith("CREATE_ORDER_SUCCESS")) {
            // Order was successful
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("✅ Order confirmed!");
                
                // Check if account credentials are included in the message
                if (message.contains("Your account has been created")) {
                    // Extract credentials from message
                    showAccountCredentialsPopup(message);
                } else {
                    // Regular success message without account creation
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Order Placed Successfully");
                    alert.setHeaderText("Your order has been placed!");
                    alert.setContentText("Your order has been created, we are working on it.\n\nExpect delivery at your selected time.\n\nThank you for choosing us!");
                    alert.showAndWait();
                    
                    // Redirect after user clicks OK
                    try {
                        // Unregister before redirecting to prevent duplicate handlers
                        cleanup();
                        App.setRoot("primary1");
                    } catch (IOException e) {
                        e.printStackTrace();
                        statusLabel.setText("❌ Failed to redirect to main page.");
                    }
                }
            });
        } else if (message.startsWith("CREATE_ORDER_FAILURE")) {
            // Order failed
            javafx.application.Platform.runLater(() -> {
                String errorMsg = message.substring(message.indexOf(":") + 1).trim();
                statusLabel.setText("❌ " + errorMsg);
            });
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
        
        // Validate phone number - exactly 10 digits, must start with "05", numbers only
        if (phone.length() != 10) {
            statusLabel.setText("❌ Phone number must be exactly 10 digits.");
            return;
        }
        if (!phone.startsWith("05")) {
            statusLabel.setText("❌ Phone number must start with '05'.");
            return;
        }
        if (!phone.matches("\\d+")) {
            statusLabel.setText("❌ Phone number can only contain numbers.");
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

            // Validate card number - exactly 16 digits
            String cardNumber = result.get()[0].trim();
            if (!cardNumber.matches("^\\d{16}$")) {
                statusLabel.setText("❌ Card number must be exactly 16 digits.");
                return;
            }

            // Validate expiration format MM/YY
            String exp = result.get()[1].trim();
            if (!exp.matches("^(0[1-9]|1[0-2])\\/\\d{2}$")) {
                statusLabel.setText("❌ Expiration must be in MM/YY format.");
                return;
            }

            // Validate CVV - exactly 3 digits
            String cvv = result.get()[2].trim();
            if (!cvv.matches("^\\d{3}$")) {
                statusLabel.setText("❌ CVV must be exactly 3 digits.");
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
            
            // Clear cart
            cart.clearCart();
            
            // The response will be handled by onWarningEvent method
            statusLabel.setText("⏳ Processing your order...");
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to send order: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            cleanup(); // Unregister before navigating away
            App.setRoot("user_menu");
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to go back to menu.");
        }
    }
    
    private void showAccountCredentialsPopup(String message) {
        // Extract username and password from message
        // Format: "CREATE_ORDER_SUCCESS;Order created successfully with ID: X! Your account has been created. Username: Y, Password: Z"
        String username = "";
        String password = "";
        
        try {
            int usernameIndex = message.indexOf("Username: ");
            int passwordIndex = message.indexOf("Password: ");
            
            if (usernameIndex != -1 && passwordIndex != -1) {
                username = message.substring(usernameIndex + "Username: ".length(), passwordIndex).trim();
                password = message.substring(passwordIndex + "Password: ".length()).trim();
                // Remove any trailing text after password
                if (password.contains(".")) {
                    password = password.substring(0, password.indexOf("."));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Welcome to our Restaurant!");
        alert.setHeaderText("Welcome to our Restaurant!");
        
        String welcomeMessage = "Your new account has been created with:\n\n" +
                "Username: " + username + "\n" +
                "Password: " + password + "\n\n" +
                "Please save these credentials for future logins.";
        
        alert.setContentText(welcomeMessage);
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
        
        // Redirect after user clicks OK
        try {
            // Unregister before redirecting to prevent duplicate handlers
            cleanup();
            App.setRoot("primary1");
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to redirect to main page.");
        }
    }
    
    // Clean up when controller is being destroyed
    public void cleanup() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
}
