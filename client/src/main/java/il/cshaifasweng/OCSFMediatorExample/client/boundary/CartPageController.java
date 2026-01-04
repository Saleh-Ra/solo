package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Cart;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

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

        // Create custom dialog with two square buttons
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Order Type");
        dialog.setHeaderText("How would you like to receive your order?");
        
        // Create two square buttons
        Button pickupButton = new Button("Pickup");
        Button deliveryButton = new Button("Delivery");
        
        // Style the buttons to be square and prominent
        pickupButton.setPrefSize(150, 150);
        deliveryButton.setPrefSize(150, 150);
        pickupButton.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: black; -fx-text-fill: white;");
        deliveryButton.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: black; -fx-text-fill: white;");
        
        // Add hover effects
        pickupButton.setOnMouseEntered(e -> pickupButton.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: #333333; -fx-text-fill: white;"));
        pickupButton.setOnMouseExited(e -> pickupButton.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: black; -fx-text-fill: white;"));
        deliveryButton.setOnMouseEntered(e -> deliveryButton.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: #333333; -fx-text-fill: white;"));
        deliveryButton.setOnMouseExited(e -> deliveryButton.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: black; -fx-text-fill: white;"));
        
        // Create HBox to hold the buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(pickupButton, deliveryButton);
        
        // Set button actions
        pickupButton.setOnAction(e -> {
            dialog.setResult("pickup");
            dialog.close();
        });
        
        deliveryButton.setOnAction(e -> {
            dialog.setResult("delivery");
            dialog.close();
        });
        
        // Set dialog content
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.getChildren().add(buttonBox);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        
        // Show dialog and handle result
        var result = dialog.showAndWait();
        if (result.isPresent()) {
            String orderType = result.get().toLowerCase();
            SimpleClient.setCurrentOrderType(orderType);
            
            try {
                App.setRoot("order");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Navigation Error", "Could not open the order screen.");
            }
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