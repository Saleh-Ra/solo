package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.client.WarningEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import javafx.application.Platform;

import java.io.IOException;

public class SignInController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Hyperlink forgotPasswordLink;

    @FXML
    private void handleSignIn() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("⚠ Please enter both username and password.");
            return;
        }

        // Send login request to server
        try {
            SimpleClient.getClient().sendToServer("LOGIN;" + username + ";" + password);
        } catch (IOException e) {
            statusLabel.setText("❌ Connection error. Please try again.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToSignUp() {
        try {
            App.setRoot("sign_up");
        } catch (IOException e) {
            statusLabel.setText("❌ Could not load sign up page.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("user_type_selection");
        } catch (IOException e) {
            statusLabel.setText("❌ Could not return to user type selection.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        // Create dialog for forgot password
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Enter your account details to reset your password");

        // Create form fields
        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Enter your phone number");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter new password");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");

        // Create grid layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone Number:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("New Password:"), 0, 2);
        grid.add(newPasswordField, 1, 2);
        grid.add(new Label("Confirm Password:"), 0, 3);
        grid.add(confirmPasswordField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType resetButtonType = new ButtonType("Reset Password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButtonType, ButtonType.CANCEL);

        // Enable/disable reset button based on field validation
        dialog.getDialogPane().lookupButton(resetButtonType).setDisable(true);
        
        // Add listeners to enable button when fields are filled
        nameField.textProperty().addListener((observable, oldValue, newValue) -> validateFields(dialog, resetButtonType, nameField, phoneField, newPasswordField, confirmPasswordField));
        phoneField.textProperty().addListener((observable, oldValue, newValue) -> validateFields(dialog, resetButtonType, nameField, phoneField, newPasswordField, confirmPasswordField));
        newPasswordField.textProperty().addListener((observable, oldValue, newValue) -> validateFields(dialog, resetButtonType, nameField, phoneField, newPasswordField, confirmPasswordField));
        confirmPasswordField.textProperty().addListener((observable, oldValue, newValue) -> validateFields(dialog, resetButtonType, nameField, phoneField, newPasswordField, confirmPasswordField));

        // Set result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == resetButtonType) {
                return new String[]{
                    nameField.getText().trim(),
                    phoneField.getText().trim(),
                    newPasswordField.getText(),
                    confirmPasswordField.getText()
                };
            }
            return null;
        });

        // Show dialog and process result
        var result = dialog.showAndWait();
        if (result.isPresent() && result.get() != null) {
            String[] data = result.get();
            String name = data[0];
            String phone = data[1];
            String newPassword = data[2];
            String confirmPassword = data[3];

            // Validate inputs
            if (name.isEmpty() || phone.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showAlert("All fields are required.");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showAlert("Passwords do not match. Please try again.");
                return;
            }

            if (newPassword.length() < 3) {
                showAlert("Password must be at least 3 characters long.");
                return;
            }

            // Register with EventBus if not already registered
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this);
            }

            // Send reset password request to server
            try {
                String request = String.format("FORGOT_PASSWORD_RESET;%s;%s;%s", name, phone, newPassword);
                SimpleClient.getClient().sendToServer(request);
                statusLabel.setText("⏳ Resetting password...");
            } catch (IOException e) {
                showAlert("Failed to connect to server. Please try again.");
                e.printStackTrace();
            }
        }
    }

    private void validateFields(Dialog<String[]> dialog, ButtonType resetButtonType, 
                                TextField nameField, TextField phoneField,
                                PasswordField newPasswordField, PasswordField confirmPasswordField) {
        boolean isValid = !nameField.getText().trim().isEmpty() &&
                         !phoneField.getText().trim().isEmpty() &&
                         !newPasswordField.getText().isEmpty() &&
                         !confirmPasswordField.getText().isEmpty() &&
                         newPasswordField.getText().equals(confirmPasswordField.getText()) &&
                         newPasswordField.getText().length() >= 3;
        
        dialog.getDialogPane().lookupButton(resetButtonType).setDisable(!isValid);
    }

    @Subscribe
    public void onWarningEvent(WarningEvent event) {
        String message = event.getWarning().getMessage();
        
        Platform.runLater(() -> {
            if (message.startsWith("FORGOT_PASSWORD_RESET_SUCCESS")) {
                String successMsg = message.substring(message.indexOf(":") + 1).trim();
                statusLabel.setText("✅ " + successMsg);
                showAlert("Password reset successfully! You can now sign in with your new password.");
                // Clear password field
                passwordField.clear();
            } else if (message.startsWith("FORGOT_PASSWORD_RESET_FAILURE")) {
                String errorMsg = message.substring(message.indexOf(":") + 1).trim();
                statusLabel.setText("❌ " + errorMsg);
                showAlert("Failed to reset password: " + errorMsg);
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password Reset");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
