package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

public class SignUpController {

    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    @FXML
    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        // Basic validation
        if (username.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("⚠ Please fill in all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("⚠ Passwords do not match.");
            return;
        }

        // Send signup request to server
        try {
            SimpleClient.getClient().sendToServer("SIGNUP;" + username + ";" + phone + ";" + password + ";" + confirmPassword);
        } catch (IOException e) {
            statusLabel.setText("❌ Connection error. Please try again.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToSignIn() {
        try {
            App.setRoot("sign_in");
        } catch (IOException e) {
            statusLabel.setText("❌ Could not load sign in page.");
            e.printStackTrace();
        }
    }
} 