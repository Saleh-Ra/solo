package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
            // TODO: Navigate to manager interface
            // For now, we'll show a placeholder message
            showInfo("Manager interface will be implemented next.");
        } catch (Exception e) {
            showError("Failed to load manager interface.");
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("🔵 UserTypeSelectionController: initialize() called");
        System.out.println("🔵 UserTypeSelectionController: FXML loaded successfully");
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
