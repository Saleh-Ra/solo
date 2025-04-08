package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Cart;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.Map;

public class CartPageController {

    @FXML
    private ListView<HBox> cartListView;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label deliveryFeeLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private Button clearCartButton;

    private Cart cart;

    @FXML
    public void initialize() {
        cart = SimpleClient.getCart();
        refreshCartView();
    }

    private void refreshCartView() {
        cartListView.getItems().clear();

        for (Map.Entry<MenuItem, Integer> entry : cart.getItems().entrySet()) {
            MenuItem item = entry.getKey();
            int quantity = entry.getValue();
            String note = cart.getNote(item);

            Label itemLabel = new Label(item.getName() + " x" + quantity + " - $" + String.format("%.2f", item.getPrice() * quantity));
            if (note != null && !note.isEmpty()) {
                itemLabel.setText(itemLabel.getText() + " [" + note + "]");
            }

            Button plusButton = new Button("+");
            Button minusButton = new Button("-");
            Button removeButton = new Button("Remove");
            Button editButton = new Button("Edit");
            
            plusButton.getStyleClass().add("small-button");
            minusButton.getStyleClass().add("small-button");
            removeButton.getStyleClass().add("small-button");
            editButton.getStyleClass().add("small-button");

            plusButton.setOnAction(e -> {
                cart.addItem(item);
                refreshCartView();
            });

            minusButton.setOnAction(e -> {
                cart.decreaseItem(item);
                refreshCartView();
            });

            removeButton.setOnAction(e -> {
                cart.removeCompletely(item);
                refreshCartView();
            });

            editButton.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(cart.getNote(item));
                dialog.setTitle("Edit Meal");
                dialog.setHeaderText("Enter special instructions for " + item.getName());
                dialog.setContentText("Note:");
                dialog.showAndWait().ifPresent(noteText -> {
                    cart.setNote(item, noteText);
                    refreshCartView();
                });
            });

            HBox row = new HBox(10, itemLabel, plusButton, minusButton, removeButton, editButton);
            cartListView.getItems().add(row);
        }

        subtotalLabel.setText("Subtotal: $" + String.format("%.2f", cart.getSubtotal()));
        
        if (cart.getItems().isEmpty()) {
            deliveryFeeLabel.setText("Delivery Fee: $0.00");
        } else {
            deliveryFeeLabel.setText("Delivery Fee: $" + String.format("%.2f", cart.getDeliveryFee()));
        }
        
        totalLabel.setText("Total: $" + String.format("%.2f", cart.calculateTotal()));
    }

    @FXML
    private void handleClearCart() {
        cart.clearCart();
        refreshCartView();
    }

    @FXML
    private void handleBackToMenu() {
        try {
            App.setRoot("user_menu");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not return to menu.");
        }
    }

    @FXML
    private void handleProceedToCheckout() {
        if (cart.getItems().isEmpty()) {
            showAlert("Cart is empty", "Please add items before continuing.");
            return;
        }

        try {
            App.setRoot("order");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not open the order screen.");
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