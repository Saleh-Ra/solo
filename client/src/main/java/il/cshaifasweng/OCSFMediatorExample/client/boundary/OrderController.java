package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class OrderController {

    @FXML
    private TextField clientIdField;

    @FXML
    private TextField branchIdField;

    @FXML
    private TextField menuItemsField;

    @FXML
    private void onOrderClicked() {
        try {
            String clientId = clientIdField.getText();
            String branchId = branchIdField.getText();
            String menuItems = menuItemsField.getText(); // expected: "1,2,5"

            if (clientId.isEmpty() || branchId.isEmpty() || menuItems.isEmpty()) {
                showAlert("יש למלא את כל השדות.");
                return;
            }

            String message = String.format("PLACE_ORDER;%s;%s;%s", clientId, branchId, menuItems);
            SimpleClient.getClient().sendToServer(message);
            showAlert("הזמנה נשלחה בהצלחה!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("אירעה שגיאה בעת שליחת ההזמנה.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("הודעה");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
