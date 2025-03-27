package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.List;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;

public class SecondaryController implements MenuUpdateListener {

    @FXML
    private ComboBox<String> mealComboBox;

    @FXML
    private TextField newPriceField;

    @FXML
    private Label statusLabel;

    @FXML
    private Button switchToPrimaryButton;

    @FXML
    private VBox menuDisplayVBox;

    @FXML
    private TextField newItemNameField;

    @FXML
    private TextField newItemIngredientsField;

    @FXML
    private TextField newItemPreferencesField;

    @FXML
    private TextField newItemPriceField;

    @FXML
    private void initialize() {
        SimpleClient.getClient().setMenuUpdateListener(this);
        mealComboBox.setValue(null);
        statusLabel.setText("");

        if (switchToPrimaryButton != null) {
            switchToPrimaryButton.setOnAction(event -> {
                try {
                    switchToPrimary();
                } catch (IOException e) {
                    e.printStackTrace();
                    statusLabel.setText("Error switching to primary view.");
                }
            });
        }

        try {
            SimpleClient.getClient().sendToServer("GET_MENU");
        } catch (IOException e) {
            statusLabel.setText("Failed to fetch menu from server.");
        }
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary1");
    }

    @FXML
    private void updatePrice() {
        String selectedMeal = mealComboBox.getValue();
        String newPriceText = newPriceField.getText();

        if (selectedMeal == null || selectedMeal.isEmpty()) {
            statusLabel.setText("Please select a meal.");
            return;
        }
        if (newPriceText == null || newPriceText.isEmpty()) {
            statusLabel.setText("Please enter a new price.");
            return;
        }

        try {
            double newPrice = Double.parseDouble(newPriceText);
            if (newPrice <= 0) {
                statusLabel.setText("Price must be a positive value.");
                return;
            }

            String request = "UPDATE_PRICE;" + selectedMeal + ";" + newPrice;
            SimpleClient.getClient().sendToServer(request);
            statusLabel.setText("Price update request sent for " + selectedMeal + ".");
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid price format. Please enter a valid number.");
        } catch (IOException e) {
            statusLabel.setText("Failed to connect to the server.");
        }
    }

    @FXML
    private void addNewItem() {
        String name = newItemNameField.getText();
        String ingredients = newItemIngredientsField.getText();
        String preferences = newItemPreferencesField.getText();
        String priceText = newItemPriceField.getText();

        if (name.isEmpty() || priceText.isEmpty()) {
            statusLabel.setText("Name and Price are required fields.");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                statusLabel.setText("Price must be a positive value.");
                return;
            }

            String request = String.format("ADD_ITEM;%s;%s;%s;%.2f", name, ingredients, preferences, price);
            SimpleClient.getClient().sendToServer(request);

            statusLabel.setText("New menu item added: " + name);
            newItemNameField.clear();
            newItemIngredientsField.clear();
            newItemPreferencesField.clear();
            newItemPriceField.clear();
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid price format. Please enter a valid number.");
        } catch (IOException e) {
            statusLabel.setText("Failed to connect to the server.");
        }
    }

    @Override
    public void onMenuUpdate(List<MenuItem> menuItems) {
        Platform.runLater(() -> {
            mealComboBox.getItems().clear();
            menuDisplayVBox.getChildren().clear();

            for (MenuItem item : menuItems) {
                mealComboBox.getItems().add(item.getName());
                Label menuItemLabel = new Label(
                        String.format("Name: %s | Ingredients: %s | Preferences: %s | Price: %.2f",
                                item.getName(), item.getIngredients(), item.getPreferences(), item.getPrice())
                );
                menuDisplayVBox.getChildren().add(menuItemLabel);
            }
        });
    }
}