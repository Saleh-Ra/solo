package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

public class SignInController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void handleSignIn() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("⚠ Please enter both email and password.");
            return;
        }

        // TODO: Validate credentials with the DB/server
        boolean credentialsAreValid = true; // Simulate success

        if (credentialsAreValid) {
            try {
                App.setRoot("personal_area");
            } catch (IOException e) {
                statusLabel.setText("❌ Failed to load personal area.");
                e.printStackTrace();
            }
        } else {
            statusLabel.setText("❌ Invalid email or password.");
        }
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("primary1");
        } catch (IOException e) {
            statusLabel.setText("❌ Could not return to main screen.");
            e.printStackTrace();
        }
    }
}
