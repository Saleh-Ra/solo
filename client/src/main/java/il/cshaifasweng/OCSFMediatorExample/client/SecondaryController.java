package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.util.List;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
import il.cshaifasweng.OCSFMediatorExample.client.WarningEvent;
import javafx.scene.control.Alert;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class SecondaryController implements MenuUpdateListener {

    @FXML
    private ComboBox<String> mealComboBox;

    @FXML
    private ComboBox<String> deleteMealComboBox;

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
    private ComboBox<String> newItemCategoryComboBox;

    @FXML
    private Label branchNameLabel;

    @FXML
    private Button reportsButton;

    @FXML
    private Button tableManagementButton;

    @FXML
    private Button complaintsButton;

    @FXML
    private VBox complaintsDisplayVBox;
    
    @FXML
    private TitledPane complaintsTitledPane;

    @FXML
    private void initialize() {
        SimpleClient.getClient().setMenuUpdateListener(this);
        mealComboBox.setValue(null);
        
        // Register with EventBus to receive complaints and warnings
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        
        // Initialize category ComboBox with available categories
        newItemCategoryComboBox.getItems().addAll("Main course", "Appetizer", "Desert", "Beverages");
        
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

        // Only fetch menu if user is a manager (not chain manager)
        String userRole = SimpleClient.getCurrentUserRole();
        if ("manager".equalsIgnoreCase(userRole)) {
            try {
                // Prefer stored selectedBranchId; if absent, derive from manager's branch name
                Integer selectedBranchId = SimpleClient.getSelectedBranchId();
                if (selectedBranchId == null) {
                    String managerBranchName = SimpleClient.getBranchName();
                    if (managerBranchName != null) {
                        switch (managerBranchName) {
                            case "Tel-Aviv":
                                selectedBranchId = 1; break;
                            case "Haifa":
                                selectedBranchId = 2; break;
                            case "Jerusalem":
                                selectedBranchId = 3; break;
                            case "Beer-Sheva":
                                selectedBranchId = 4; break;
                            default: {
                                String[] parts = managerBranchName.split("\\D+");
                                for (String part : parts) {
                                    if (!part.isEmpty()) {
                                        try {
                                            selectedBranchId = Integer.parseInt(part);
                                            break;
                                        } catch (NumberFormatException ignored) {}
                                    }
                                }
                            }
                        }
                        if (selectedBranchId != null) {
                            SimpleClient.setSelectedBranchId(selectedBranchId);
                        }
                    }
                }

                if (selectedBranchId != null) {
                    SimpleClient.getClient().sendToServer("GET_MENU;" + selectedBranchId);
                    System.out.println("SecondaryController: Requesting menu for branch ID: " + selectedBranchId);
                } else {
                    // Fallback: universal only
                    SimpleClient.getClient().sendToServer("GET_MENU");
                    System.out.println("SecondaryController: No branch resolved, requesting universal menu only");
                }
            } catch (IOException e) {
                statusLabel.setText("Failed to fetch menu from server.");
            }
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
            String category = newItemCategoryComboBox.getValue();
            String priceText = newItemPriceField.getText();

            if (name.isEmpty() || priceText.isEmpty() || category == null || category.isEmpty()) {
                statusLabel.setText("Name, Category, and Price are required fields.");
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

            String request = String.format("ADD_ITEM;%s;%s;%s;%s;%.2f;%d", name, ingredients, preferences, category, price, branchId);
            System.out.println("Sending add item request: " + request);
            SimpleClient.getClient().sendToServer(request);

            statusLabel.setText("New menu item added: " + name + " for branch ID: " + branchId);
            newItemNameField.clear();
            newItemIngredientsField.clear();
            newItemPreferencesField.clear();
            newItemCategoryComboBox.setValue(null);
            newItemPriceField.clear();
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid price format. Please enter a valid number.");
        } catch (IOException e) {
            statusLabel.setText("Failed to connect to the server.");
        }
    }

    @FXML
    private void handleViewReports() {
        try {
            App.setRoot("reports_view");
        } catch (IOException e) {
            showError("Error opening reports: " + e.getMessage());
        }
    }

    @FXML
    private void handleTableManagement() {
        try {
            String userRole = SimpleClient.getCurrentUserRole();
            
            // If user is a regular manager, automatically use their branch
            if ("manager".equalsIgnoreCase(userRole)) {
                String managerBranchName = SimpleClient.getBranchName();
                if (managerBranchName != null) {
                    // Map branch name to branch ID
                    Integer branchId = getBranchIdFromName(managerBranchName);
                    if (branchId != null) {
                        // Set the branch and go directly to table management
                        SimpleClient.setSelectedBranchId(branchId);
                        SimpleClient.setSelectedBranchName(managerBranchName);
                        App.setRoot("tables_map");
                        return;
                    }
                }
                showError("Unable to determine your branch. Please contact support.");
            } else if ("chain_manager".equalsIgnoreCase(userRole)) {
                // Chain managers can choose any branch
                App.setRoot("reservation_branch_selection");
            } else {
                showError("Table management is only available for managers.");
            }
        } catch (IOException e) {
            showError("Error opening table management: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to get branch ID from branch name
     */
    private Integer getBranchIdFromName(String branchName) {
        if (branchName == null) {
            return null;
        }
        
        switch (branchName) {
            case "Tel-Aviv":
                return 1;
            case "Haifa":
                return 2;
            case "Jerusalem":
                return 3;
            case "Beer-Sheva":
                return 4;
            default:
                // Try to extract number from branch name
                String[] parts = branchName.split("\\D+");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        try {
                            return Integer.parseInt(part);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                return null;
        }
    }

    @Override
    public void onMenuUpdate(List<MenuItem> menuItems) {
        Platform.runLater(() -> {
            mealComboBox.getItems().clear();
            if (deleteMealComboBox != null) {
                deleteMealComboBox.getItems().clear();
            }
            menuDisplayVBox.getChildren().clear();

            for (MenuItem item : menuItems) {
                mealComboBox.getItems().add(item.getName());
                if (deleteMealComboBox != null) {
                    deleteMealComboBox.getItems().add(item.getName());
                }
                
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
                
                Label categoryLabel = new Label(
                    String.format("Category: %s", item.getCategory())
                );
                categoryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #228B22;");
                
                Label priceLabel = new Label(
                    String.format("Price: $%.2f", item.getPrice())
                );
                priceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3f87a6;");

                // Manager-only action buttons: toggle scope and edit ingredients
                Button toggleScopeBtn = new Button("Toggle Scope");
                toggleScopeBtn.setStyle("-fx-font-size: 12px; -fx-background-color: #795548; -fx-text-fill: white;");
                toggleScopeBtn.setOnAction(e -> toggleItemScope(item));

                Button editIngredientsBtn = new Button("Edit Ingredients");
                editIngredientsBtn.setStyle("-fx-font-size: 12px; -fx-background-color: #607D8B; -fx-text-fill: white;");
                editIngredientsBtn.setOnAction(e -> editItemIngredients(item));

                HBox actionsRow = new HBox(8, toggleScopeBtn, editIngredientsBtn);

                itemBox.getChildren().addAll(nameLabel, detailsLabel, prefsLabel, categoryLabel, priceLabel, actionsRow);
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

    private void toggleItemScope(MenuItem item) {
        try {
            Integer branchId = SimpleClient.getSelectedBranchId();
            if (branchId == null) {
                statusLabel.setText("Cannot toggle scope: branch not resolved.");
                return;
            }
            String request = String.format("TOGGLE_ITEM_SCOPE;%s;%d", item.getName(), branchId);
            SimpleClient.getClient().sendToServer(request);
            statusLabel.setText("Toggle scope request sent for " + item.getName());
        } catch (IOException ex) {
            statusLabel.setText("Failed to connect to the server.");
        }
    }

    private void editItemIngredients(MenuItem item) {
        TextInputDialog dialog = new TextInputDialog(item.getIngredients());
        dialog.setTitle("Edit Ingredients");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter new ingredients:");
        dialog.showAndWait().ifPresent(newIngredients -> {
            if (newIngredients == null || newIngredients.trim().isEmpty()) {
                statusLabel.setText("Ingredients cannot be empty.");
                return;
            }
            try {
                String request = String.format("UPDATE_INGREDIENTS;%s;%s", item.getName(), newIngredients.trim());
                SimpleClient.getClient().sendToServer(request);
                statusLabel.setText("Update ingredients request sent for " + item.getName());
            } catch (IOException e) {
                statusLabel.setText("Failed to connect to the server.");
            }
        });
    }

    @FXML
    private void deleteSelectedItem() {
        String selectedMeal = deleteMealComboBox != null ? deleteMealComboBox.getValue() : null;
        if (selectedMeal == null || selectedMeal.isEmpty()) {
            statusLabel.setText("Please select a meal to delete.");
            return;
        }

        try {
            String request = "DELETE_ITEM;" + selectedMeal;
            SimpleClient.getClient().sendToServer(request);
            statusLabel.setText("Delete request sent for " + selectedMeal + ".");
        } catch (IOException e) {
            statusLabel.setText("Failed to connect to the server.");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleViewComplaints() {
        try {
            String role = SimpleClient.getCurrentUserRole();
            if ("chain_manager".equalsIgnoreCase(role)) {
                // Chain manager can see all complaints
                SimpleClient.getClient().sendToServer("GET_ALL_COMPLAINTS");
                statusLabel.setText("Loading all complaints...");
            } else if ("manager".equalsIgnoreCase(role)) {
                // Branch manager can see complaints for their branch
                Integer branchId = getBranchId();
                if (branchId != null) {
                    SimpleClient.getClient().sendToServer("GET_BRANCH_COMPLAINTS;" + branchId);
                    statusLabel.setText("Loading complaints for your branch...");
                } else {
                    statusLabel.setText("Could not determine branch ID.");
                }
            } else {
                statusLabel.setText("You don't have permission to view complaints.");
            }
        } catch (IOException e) {
            statusLabel.setText("Failed to request complaints: " + e.getMessage());
        }
    }

    private Integer getBranchId() {
        String branchName = SimpleClient.getBranchName();
        if (branchName == null) {
            return null;
        }
        switch (branchName) {
            case "Tel-Aviv": return 1;
            case "Haifa": return 2;
            case "Jerusalem": return 3;
            case "Beer-Sheva": return 4;
            default: return null;
        }
    }

    @Subscribe
    public void onComplaintsReceived(SimpleClient.ComplaintsReceivedEvent event) {
        Platform.runLater(() -> {
            List<Complaint> complaints = event.getComplaints();
            displayComplaints(complaints);
        });
    }

    @Subscribe
    public void onWarningEvent(WarningEvent event) {
        String message = event.getWarning().getMessage();
        
        if (message.startsWith("RESPOND_TO_COMPLAINT_SUCCESS")) {
            Platform.runLater(() -> {
                String successMsg = message.substring(message.indexOf(":") + 1).trim();
                statusLabel.setText("✅ " + successMsg);
                // Refresh complaints list
                handleViewComplaints();
            });
        } else if (message.startsWith("RESPOND_TO_COMPLAINT_FAILURE")) {
            Platform.runLater(() -> {
                String errorMsg = message.substring(message.indexOf(":") + 1).trim();
                statusLabel.setText("❌ " + errorMsg);
                showError("Failed to respond to complaint: " + errorMsg);
            });
        }
    }

    private void displayComplaints(List<Complaint> complaints) {
        complaintsDisplayVBox.getChildren().clear();
        
        // Expand the complaints TitledPane so user can see the complaints
        if (complaintsTitledPane != null) {
            complaintsTitledPane.setExpanded(true);
        }
        
        if (complaints == null || complaints.isEmpty()) {
            Label noComplaintsLabel = new Label("No complaints found.");
            noComplaintsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            complaintsDisplayVBox.getChildren().add(noComplaintsLabel);
            statusLabel.setText("No complaints to display.");
            return;
        }

        statusLabel.setText("Found " + complaints.size() + " complaint(s).");
        
        for (Complaint complaint : complaints) {
            VBox complaintBox = new VBox(10);
            complaintBox.setStyle("-fx-background-color: #fff; -fx-padding: 15px; -fx-border-color: #ddd; -fx-border-radius: 5px;");
            
            // Complaint info
            Label idLabel = new Label("Complaint #" + complaint.getId());
            idLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            Label clientLabel = new Label("From: " + complaint.getClient().getName() + 
                " (" + complaint.getClient().getAccount().getPhoneNumber() + ")");
            clientLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            
            Label branchLabel = new Label("Branch: " + complaint.getBranch().getLocation());
            branchLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            
            Label statusLabel = new Label("Status: " + complaint.getStatus());
            String statusColor = complaint.getStatus() == Complaint.Status.PENDING ? "#e74c3c" : 
                                complaint.getStatus() == Complaint.Status.RESOLVED ? "#27ae60" : "#95a5a6";
            statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");
            
            Label descriptionLabel = new Label("Description: " + complaint.getDescription());
            descriptionLabel.setWrapText(true);
            descriptionLabel.setStyle("-fx-font-size: 14px;");
            
            // Show manager notes if exists
            if (complaint.getManagerNotes() != null && !complaint.getManagerNotes().isEmpty()) {
                Label notesLabel = new Label("Response: " + complaint.getManagerNotes());
                notesLabel.setWrapText(true);
                notesLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50; -fx-font-style: italic;");
                complaintBox.getChildren().add(notesLabel);
            }
            
            // Response button (only for PENDING complaints)
            if (complaint.getStatus() == Complaint.Status.PENDING) {
                Button respondButton = new Button("Respond");
                respondButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                respondButton.setOnAction(e -> showRespondDialog(complaint));
                complaintBox.getChildren().add(respondButton);
            }
            
            complaintBox.getChildren().addAll(idLabel, clientLabel, branchLabel, statusLabel, descriptionLabel);
            complaintsDisplayVBox.getChildren().add(complaintBox);
        }
    }

    private void showRespondDialog(Complaint complaint) {
        javafx.scene.control.Dialog<String[]> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Respond to Complaint");
        dialog.setHeaderText("Complaint #" + complaint.getId() + " from " + complaint.getClient().getName());

        javafx.scene.control.Label responseLabel = new javafx.scene.control.Label("Response:");
        javafx.scene.control.TextArea responseField = new javafx.scene.control.TextArea();
        responseField.setPromptText("Enter your response...");
        responseField.setPrefRowCount(5);
        responseField.setWrapText(true);

        javafx.scene.control.Label statusLabelDialog = new javafx.scene.control.Label("Status:");
        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("RESOLVED", "REJECTED", "PENDING");
        statusComboBox.setValue("RESOLVED");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        grid.add(responseLabel, 0, 0);
        grid.add(responseField, 1, 0);
        grid.add(statusLabelDialog, 0, 1);
        grid.add(statusComboBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new String[]{responseField.getText().trim(), statusComboBox.getValue()};
            }
            return null;
        });

        var result = dialog.showAndWait();
        if (result.isPresent() && result.get() != null) {
            String[] responseData = result.get();
            String response = responseData[0];
            String status = responseData[1];

            if (response.isEmpty()) {
                showError("Response cannot be empty.");
                return;
            }

            try {
                String request = String.format("RESPOND_TO_COMPLAINT;%d;%s;%s", 
                    complaint.getId(), response, status);
                SimpleClient.getClient().sendToServer(request);
                this.statusLabel.setText("Response sent. Refreshing complaints...");
                // Refresh complaints after a short delay
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(500);
                        handleViewComplaints();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                showError("Failed to send response: " + e.getMessage());
            }
        }
    }
}