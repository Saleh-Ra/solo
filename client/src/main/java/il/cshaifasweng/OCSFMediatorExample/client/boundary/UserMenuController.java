package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Cart;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class UserMenuController {

    @FXML
    private VBox menuDisplayVBox;

    @FXML
    private ListView<HBox> cartListView;

    @FXML
    private Label totalLabel;

    @FXML
    private Button clearCartButton;

    @FXML
    private Button placeOrderButton;

    private Cart cart;

    @FXML
    public void initialize() {
        cart = SimpleClient.getCart();

        // 🔁 Always request latest menu from server
        try {
            SimpleClient.getClient().sendToServer("GET_MENU");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ✅ Register menu update listener
        SimpleClient.getClient().setMenuUpdateListener(menuItems ->
                Platform.runLater(this::loadMenu)
        );

        refreshCartView();
    }

    private void loadMenu() {
        menuDisplayVBox.getChildren().clear();
        List<MenuItem> items = SimpleClient.getMenuItems();

        for (MenuItem item : items) {
            Label nameLabel = new Label(item.getName() + " - $" + String.format("%.2f", item.getPrice()));
            Button addButton = new Button("Add to Cart");

            addButton.setOnAction(event -> {
                cart.addItem(item);
                refreshCartView();
            });

            HBox row = new HBox(10, nameLabel, addButton);
            menuDisplayVBox.getChildren().add(row);
        }
    }

    private void refreshCartView() {
        cartListView.getItems().clear();

        for (MenuItem item : cart.getItems()) {
            Label itemLabel = new Label(item.getName() + " - $" + String.format("%.2f", item.getPrice()));
            Button removeButton = new Button("Remove");

            removeButton.setOnAction(event -> {
                cart.removeItem(item);
                refreshCartView();
            });

            HBox row = new HBox(10, itemLabel, removeButton);
            cartListView.getItems().add(row);
        }

        totalLabel.setText("Total: $" + String.format("%.2f", cart.calculateTotal()));
    }

    @FXML
    private void handleClearCart() {
        cart.clearCart();
        refreshCartView();
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
    private void handleBackToMainPage() {
        try {
            App.setRoot("primary1");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not return to main menu.");
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
