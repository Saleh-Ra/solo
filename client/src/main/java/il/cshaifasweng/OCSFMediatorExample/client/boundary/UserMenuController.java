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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.io.IOException;
import java.util.List;

public class UserMenuController {

    @FXML
    private VBox menuDisplayVBox;

    @FXML
    private Label cartCountLabel;
    
    @FXML
    private Button cartButton;

    @FXML
    private ComboBox<String> categoryFilterCombo;

    private Cart cart;

    @FXML
    public void initialize() {
        cart = SimpleClient.getCart();

        try {
            // Get the selected branch ID and send it with the menu request
            Integer selectedBranchId = SimpleClient.getSelectedBranchId();
            if (selectedBranchId != null) {
                SimpleClient.getClient().sendToServer("GET_MENU;" + selectedBranchId);
                System.out.println("Requesting menu for branch ID: " + selectedBranchId);
            } else {
                SimpleClient.getClient().sendToServer("GET_MENU");
                System.out.println("No branch selected, requesting default menu only");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        SimpleClient.getClient().setMenuUpdateListener(menuItems ->
                Platform.runLater(() -> {
                    // Populate category filter once we have items
                    if (categoryFilterCombo != null && categoryFilterCombo.getItems().isEmpty()) {
                        categoryFilterCombo.getItems().clear();
                        categoryFilterCombo.getItems().add("All");
                        for (MenuItem item : SimpleClient.getMenuItems()) {
                            String cat = item.getCategory();
                            if (cat != null && !categoryFilterCombo.getItems().contains(cat)) {
                                categoryFilterCombo.getItems().add(cat);
                            }
                        }
                        categoryFilterCombo.getSelectionModel().selectFirst();
                    }
                    loadMenu();
                })
        );
        
        updateCartCount();
    }

    @FXML
    private void handleCategoryChanged() {
        if (categoryFilterCombo == null) {
            return;
        }
        String selected = categoryFilterCombo.getValue();
        try {
            Integer selectedBranchId = SimpleClient.getSelectedBranchId();
            if (selected == null || selected.isEmpty() || "All".equalsIgnoreCase(selected)) {
                if (selectedBranchId != null) {
                    SimpleClient.getClient().sendToServer("GET_MENU;" + selectedBranchId);
                } else {
                    SimpleClient.getClient().sendToServer("GET_MENU");
                }
            } else {
                // Filter by category on the server, include branch if available
                if (selectedBranchId != null) {
                    SimpleClient.getClient().sendToServer("GET_MENU_BY_CATEGORY;" + selected + ";" + selectedBranchId);
                } else {
                    SimpleClient.getClient().sendToServer("GET_MENU_BY_CATEGORY;" + selected);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            boolean isSpecialItem = item.getBranch() != null;

            // Build a large image button per item
            Button itemButton = new Button();
            
            // Add special badge to text if it's a special item
            String itemText = isSpecialItem 
                ? "⭐ " + item.getName() + " ⭐\n(" + item.getCategory() + ")\n$" + String.format("%.2f", item.getPrice())
                : item.getName() + " (" + item.getCategory() + ")\n$" + String.format("%.2f", item.getPrice());
            
            itemButton.setText(itemText);
            itemButton.setWrapText(true);
            itemButton.setContentDisplay(ContentDisplay.TOP);
            itemButton.setAlignment(Pos.CENTER);
            
            // Apply different styling based on whether it's special
            if (isSpecialItem) {
                // Special items: Gold gradient background with special border
                itemButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a; " +
                    "-fx-background-color: linear-gradient(to bottom, #ffd700, #ffed4e, #ffd700); " +
                    "-fx-border-color: #d4af37; -fx-border-width: 3; -fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(212,175,55,0.6), 10, 0, 0, 3);");
            } else {
                // Regular items: Simple white background
                itemButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #222222; " +
                    "-fx-background-color: white; -fx-border-color: #dddddd; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8;");
            }
            
            itemButton.setPadding(new Insets(10));

            ImageView imageView = createImageViewForItem(item);
            if (imageView != null) {
                itemButton.setGraphic(imageView);
                itemButton.setGraphicTextGap(8);
            } else {
                // Ensure text is visible and layout is compact when no image is available
                itemButton.setContentDisplay(ContentDisplay.TEXT_ONLY);
                if (!isSpecialItem) {
                    itemButton.setStyle(itemButton.getStyle() + " -fx-background-color: linear-gradient(#ffffff, #f7f7f7);");
                }
            }

            itemButton.setOnAction(event -> {
                cart.addItem(item);
                updateCartCount();
            });

            menuDisplayVBox.getChildren().add(itemButton);
        }
    }

    private ImageView createImageViewForItem(MenuItem item) {
        Image image = null;

        // 1) Use explicit imagePath if provided on the entity
        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
            image = tryLoad("/il/cshaifasweng/OCSFMediatorExample/client/images/" + item.getImagePath());
        }

        // 2) Try common filename patterns based on item name
        if (image == null) {
            String name = item.getName();
            if (name != null && !name.isEmpty()) {
                String base = name.trim();
                String lower = base.toLowerCase();

                String[] candidates = new String[] {
                    base + ".jpg",
                    base + ".png",
                    lower + ".jpg",
                    lower + ".png",
                    lower.replace(" ", "-") + ".jpg",
                    lower.replace(" ", "-") + ".png",
                    lower.replace(" ", "_") + ".jpg",
                    lower.replace(" ", "_") + ".png"
                };

                for (String candidate : candidates) {
                    image = tryLoad("/il/cshaifasweng/OCSFMediatorExample/client/images/" + candidate);
                    if (image != null) break;
                }
            }
        }

        // 3) Try category-based fallback
        if (image == null && item.getCategory() != null && !item.getCategory().isEmpty()) {
            String cat = item.getCategory().trim();
            String lowerCat = cat.toLowerCase();
            String[] catCandidates = new String[] {
                lowerCat + ".jpg",
                lowerCat + ".png",
                lowerCat.replace(" ", "-") + ".jpg",
                lowerCat.replace(" ", "-") + ".png"
            };
            for (String candidate : catCandidates) {
                image = tryLoad("/il/cshaifasweng/OCSFMediatorExample/client/images/" + candidate);
                if (image != null) break;
            }
        }

        // 4) Final default fallback
        if (image == null) {
            image = tryLoad("/il/cshaifasweng/OCSFMediatorExample/client/images/default_meal.jpg");
        }

        if (image == null) {
            return null;
        }

        ImageView iv = new ImageView(image);
        iv.setPreserveRatio(true);
        iv.setFitWidth(240);
        iv.setFitHeight(160);
        return iv;
    }

    private Image tryLoad(String resourcePath) {
        try {
            java.io.InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is != null) {
                return new Image(is);
            }
        } catch (Exception ignored) { }
        return null;
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
