package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.io.IOException;

public class DeliveryOrderController {

    @FXML
    private TextField clientIdField;

    @FXML
    private TextField branchIdField;

    @FXML
    private TextField menuItemsField;

    @FXML
    private TextField deliveryAddressField;

    @FXML
    private void onSubmitClicked() {
        try {
            String clientId = clientIdField.getText().trim();
            String branchId = branchIdField.getText().trim();
            String menuItems = menuItemsField.getText().trim();
            String address = deliveryAddressField.getText().trim();

            if (clientId.isEmpty() || branchId.isEmpty() || menuItems.isEmpty() || address.isEmpty()) {
                System.out.println("Please fill in all fields.");
                return;
            }

            String message = String.format("DELIVERY_ORDER;%s;%s;%s;%s", clientId, branchId, menuItems, address);
            SimpleClient.getClient().sendToServer(message);

            System.out.println("Delivery order sent: " + message);
        } catch (IOException e) {
            System.out.println("Failed to send delivery order");
            e.printStackTrace();
        }
    }
}
