package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UserTypeSelectionController implements Initializable {

    @FXML
    void handleGuestSelection() {
        try {
            // Navigate to the main customer interface (primary1)
            App.setRoot("primary1");
        } catch (IOException e) {
            showError("Failed to load customer interface.");
            e.printStackTrace();
        }
    }

    @FXML
    void handleManagerSelection() {
        try {
            // Navigate to sign-in page for manager authentication
            App.setRoot("sign_in");
        } catch (IOException e) {
            showError("Failed to load sign-in page.");
            e.printStackTrace();
        }
    }

    @FXML
    void handleNewUserHelp() {
        String helpMessage = "📱 New User Guide\n\n" +
                "Welcome to Restaurant Kitchen!\n\n" +
                "When you make a reservation or place an order, you can track it in your Personal Area.\n\n" +
                "🔑 Access Your Personal Area:\n" +
                "• Username: Your phone number\n" +
                "• Password: Your name + 123\n\n" +
                "Example:\n" +
                "If your name is 'John' and phone is '0501234567':\n" +
                "• Username: 0501234567\n" +
                "• Password: John123\n\n" +
                "💡 Tip: You can view and manage all your reservations and orders from your Personal Area!";
        
        showInfo(helpMessage);
    }

    @FXML
    private Button newUserHelpButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("🔵 UserTypeSelectionController: initialize() called");
        System.out.println("🔵 UserTypeSelectionController: FXML loaded successfully");
        
        // Bring the help button to front to ensure it's clickable
        if (newUserHelpButton != null) {
            newUserHelpButton.toFront();
        }
    }

    private void showError(String msg) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
