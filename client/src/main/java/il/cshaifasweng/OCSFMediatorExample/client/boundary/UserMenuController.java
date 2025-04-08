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
    private Label cartCountLabel;
    
    @FXML
    private Button cartButton;

    private Cart cart;

    @FXML
    public void initialize() {
        cart = SimpleClient.getCart();

        try {
            SimpleClient.getClient().sendToServer("GET_MENU");
        } catch (IOException e) {
            e.printStackTrace();
        }

        SimpleClient.getClient().setMenuUpdateListener(menuItems ->
                Platform.runLater(this::loadMenu)
        );
        
        updateCartCount();
    }

    private void loadMenu() {
        menuDisplayVBox.getChildren().clear();
        List<MenuItem> items = SimpleClient.getMenuItems();

        System.out.println("Loading menu with " + items.size() + " items");
        
        // Debug: Print all menu items to check if branch information is available
        for (MenuItem item : items) {
            System.out.println("Menu item: " + item.getName() + 
                             ", ID: " + item.getId() + 
                             ", Branch: " + (item.getBranch() != null ? item.getBranch().getId() : "Default"));
        }

        for (MenuItem item : items) {
            // Check if this is a branch-specific item
            boolean isSpecialItem = item.getBranch() != null;
            
            // Create label with appropriate styling
            Label nameLabel;
            if (isSpecialItem) {
                nameLabel = new Label("★ SPECIAL: " + item.getName() + " (" + item.getCategory() + ") - $" + String.format("%.2f", item.getPrice()));
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3f87a6;");
            } else {
                nameLabel = new Label(item.getName() + " (" + item.getCategory() + ") - $" + String.format("%.2f", item.getPrice()));
            }
            
            Button addButton = new Button("Add to Cart");
            addButton.getStyleClass().add("small-button");

            addButton.setOnAction(event -> {
                cart.addItem(item);
                updateCartCount();
            });

            HBox row = new HBox(10, nameLabel, addButton);
            
            // Add special background for branch-specific items
            if (isSpecialItem) {
                row.setStyle("-fx-background-color: rgba(63, 135, 166, 0.1); -fx-padding: 5; -fx-background-radius: 5;");
            }
            
            menuDisplayVBox.getChildren().add(row);
        }
    }
    
    private void updateCartCount() {
        int itemCount = 0;
        for (Integer quantity : cart.getItems().values()) {
            itemCount += quantity;
        }
        cartCountLabel.setText(String.valueOf(itemCount));
    }

    @FXML
    private void handleViewCart() {
        try {
            App.setRoot("cart_page");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open cart page.");
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
