package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

public class SignInController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

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
}
