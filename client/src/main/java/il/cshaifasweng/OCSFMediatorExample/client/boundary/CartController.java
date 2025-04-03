package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Cart;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.util.Map;

public class CartController {

    @FXML
    private ListView<String> cartListView;

    @FXML
    private Label totalCostLabel;

    @FXML
    private Button placeOrderButton;

    @FXML
    private Button clearCartButton;

    private Cart cart;

    @FXML
    public void initialize() {
        cart = SimpleClient.getCart(); // Shared cart from client
        refreshCartView();
    }

    private void refreshCartView() {
        cartListView.getItems().clear();
        double total = 0;

        for (Map.Entry<MenuItem, Integer> entry : cart.getItems().entrySet()) {
            MenuItem item = entry.getKey();
            int quantity = entry.getValue();
            double itemTotal = item.getPrice() * quantity;

            cartListView.getItems().add(
                    String.format("%s x%d - $%.2f", item.getName(), quantity, itemTotal)
            );

            total += itemTotal;
        }

        totalCostLabel.setText("Total: $" + String.format("%.2f", total));
    }

    @FXML
    private void handlePlaceOrder() {
        if (cart.getItems().isEmpty()) {
            showAlert("Cart is empty", "Please add items before placing an order.");
            return;
        }

        try {
            SimpleClient.getClient().placeOrder(cart);
            showAlert("Order Placed", "Your order was sent to the server!");
            cart.clearCart();
            refreshCartView();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not place order: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearCart() {
        cart.clearCart();
        refreshCartView();
    }

    @FXML
    private void handleBackToMenu() {
        try {
            App.setRoot("secondary2");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
