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
    private Label branchNameLabel;

    @FXML
    private void initialize() {
        SimpleClient.getClient().setMenuUpdateListener(this);
        mealComboBox.setValue(null);
        
        // Display welcome message and branch name for each role
        String role = SimpleClient.getCurrentUserRole();
        if (role != null) {
            String branchName = SimpleClient.getClient().getBranchName();
            
            switch (role.toLowerCase()) {
                case "manager":
                    if (branchName != null && !branchName.isEmpty()) {
                        branchNameLabel.setText(" - " + branchName);
                        statusLabel.setText("Welcome Branch Manager! You can update menu items and prices.");
                    } else {
                        branchNameLabel.setText(" - Unknown Branch");
                        statusLabel.setText("Branch information not available.");
                    }
                    break;
                    
                case "chain_manager":
                    branchNameLabel.setText(" - Chain Manager");
                    statusLabel.setText("Welcome Chain Manager! You have access to all restaurant branches.");
                    break;
                    
                case "customer_support":
                    if (branchName != null && !branchName.isEmpty()) {
                        branchNameLabel.setText(" - " + branchName);
                        statusLabel.setText("Welcome Customer Support! You can handle customer inquiries for this branch.");
                    } else {
                        branchNameLabel.setText(" - Support Staff");
                        statusLabel.setText("Branch information not available.");
                    }
                    break;
                    
                case "nutritionist":
                    if (branchName != null && !branchName.isEmpty()) {
                        branchNameLabel.setText(" - " + branchName);
                        statusLabel.setText("Welcome Nutritionist! You can manage dietary information for this branch.");
                    } else {
                        branchNameLabel.setText(" - Nutritionist");
                        statusLabel.setText("Branch information not available.");
                    }
                    break;
                    
                default:
                    branchNameLabel.setText("");
                    statusLabel.setText("");
                    break;
            }
        } else {
            branchNameLabel.setText("");
            statusLabel.setText("");
        }

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
        try {
            String name = newItemNameField.getText();
            String ingredients = newItemIngredientsField.getText();
            String preferences = newItemPreferencesField.getText();
            String priceText = newItemPriceField.getText();

            if (name.isEmpty() || priceText.isEmpty()) {
                statusLabel.setText("Name and Price are required fields.");
                return;
            }

            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                statusLabel.setText("Price must be a positive value.");
                return;
            }

            // Get the manager's branch ID
            Integer branchId = null;
            String role = SimpleClient.getCurrentUserRole();
            if ("manager".equalsIgnoreCase(role)) {
                if (SimpleClient.getClient().getBranchName() != null) {
                    // Get branch ID from the branch name
                    try {
                        // This is a simple approach - you might need to query the server for the actual branch ID
                        // Tel-Aviv -> 1, Haifa -> 2, Jerusalem -> 3, Beer-Sheva -> 4
                        String branchName = SimpleClient.getClient().getBranchName();
                        switch (branchName) {
                            case "Tel-Aviv":
                                branchId = 1;
                                break;
                            case "Haifa":
                                branchId = 2;
                                break;
                            case "Jerusalem":
                                branchId = 3;
                                break;
                            case "Beer-Sheva":
                                branchId = 4;
                                break;
                            default:
                                // Try to extract a number if the branch name contains one
                                String[] parts = branchName.split("\\D+");
                                for (String part : parts) {
                                    if (!part.isEmpty()) {
                                        try {
                                            branchId = Integer.parseInt(part);
                                            break;
                                        } catch (NumberFormatException e) {
                                            // Continue trying other parts
                                        }
                                    }
                                }
                                // If still null, default to 1
                                if (branchId == null) {
                                    branchId = 1;
                                }
                        }
                    } catch (Exception e) {
                        statusLabel.setText("Could not determine branch ID. Item not added.");
                        return;
                    }
                }
            }

            if (branchId == null) {
                statusLabel.setText("Branch ID is required for adding items. Please contact support.");
                return;
            }

            String request = String.format("ADD_ITEM;%s;%s;%s;%.2f;%d", name, ingredients, preferences, price, branchId);
            System.out.println("Sending add item request: " + request);
            SimpleClient.getClient().sendToServer(request);

            statusLabel.setText("New menu item added: " + name + " for branch ID: " + branchId);
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
                
                // Create a styled menu item display
                VBox itemBox = new VBox(5);
                itemBox.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #dddddd; " +
                                "-fx-border-radius: 5; -fx-background-radius: 5;");
                
                Label nameLabel = new Label(item.getName());
                nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                
                Label detailsLabel = new Label(
                    String.format("Ingredients: %s", item.getIngredients())
                );
                detailsLabel.setStyle("-fx-font-size: 14px;");
                
                Label prefsLabel = new Label(
                    String.format("Preferences: %s", item.getPreferences())
                );
                prefsLabel.setStyle("-fx-font-size: 14px;");
                
                Label priceLabel = new Label(
                    String.format("Price: $%.2f", item.getPrice())
                );
                priceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3f87a6;");
                
                itemBox.getChildren().addAll(nameLabel, detailsLabel, prefsLabel, priceLabel);
                menuDisplayVBox.getChildren().add(itemBox);
            }
            
            // Update the status message to show item count
            if (!menuItems.isEmpty()) {
                String role = SimpleClient.getCurrentUserRole();
                if ("manager".equalsIgnoreCase(role)) {
                    String branchName = SimpleClient.getClient().getBranchName();
                    if (branchName != null && !branchName.isEmpty()) {
                        statusLabel.setText(String.format("Showing %d menu items - You can manage all items", menuItems.size()));
                    } else {
                        statusLabel.setText(String.format("Showing %d menu items - You have chain-wide access", menuItems.size()));
                    }
                }
            }
        });
    }
}