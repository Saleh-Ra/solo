package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.TablesReceivedEvent;
import il.cshaifasweng.OCSFMediatorExample.client.WarningEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.RestaurantTable;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.geometry.Insets;

public class TableDiagramController {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<LocalTime> timeComboBox;
    @FXML private Spinner<Integer> guestsSpinner;
    @FXML private TextField phoneField;
    @FXML private Button reserveButton;
    @FXML private Label statusLabel;
    @FXML private Label selectedTableLabel;
    @FXML private Label branchLabel; // New label to show selected branch
    
    @FXML private GridPane barAreaGrid;
    @FXML private GridPane insideAreaGrid;
    @FXML private GridPane outsideAreaGrid;
    
    // Store the currently selected table reference
    private TablePane selectedTable = null;
    
    // Branch information from previous selection
    private int selectedBranchId;
    private String selectedBranchName;
    
    @FXML
    public void initialize() {
        System.out.println("🔵 TableDiagramController: initialize() called");
        
        // Register with EventBus
        EventBus.getDefault().register(this);
        System.out.println("🔵 TableDiagramController: Registered with EventBus");
        
        // Get the branch selected from the previous step
        selectedBranchId = SimpleClient.getSelectedBranchId();
        selectedBranchName = SimpleClient.getSelectedBranchName();
        
        System.out.println("🔵 TableDiagramController: Selected branch ID: " + selectedBranchId + ", Name: " + selectedBranchName);
        
        if (selectedBranchId == 0 || selectedBranchName == null) {
            // No branch selected, go back to main menu
            System.out.println("❌ TableDiagramController: No branch selected, redirecting to main menu");
            try {
                App.setRoot("primary1");
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        
        // Display the selected branch
        branchLabel.setText("📍 " + selectedBranchName + " Branch");
        System.out.println("🔵 TableDiagramController: Branch label set to: " + selectedBranchName);
        
        // Initialize date picker to today
        datePicker.setValue(LocalDate.now());
        
        // Set up time combo box with 15-minute intervals
        List<LocalTime> timeSlots = new ArrayList<>();
        LocalTime start = LocalTime.of(9, 0); // 9:00 AM
        LocalTime end = LocalTime.of(23, 0);  // 11:00 PM
        
        while (start.isBefore(end)) {
            timeSlots.add(start);
            start = start.plusMinutes(15);
        }
        
        timeComboBox.setItems(FXCollections.observableArrayList(timeSlots));
        timeComboBox.setConverter(new StringConverter<>() {
            private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            @Override
            public String toString(LocalTime time) {
                if (time == null) {
                    return "";
                }
                return timeFormatter.format(time);
            }
            
            @Override
            public LocalTime fromString(String string) {
                if (string == null || string.isEmpty()) {
                    return null;
                }
                return LocalTime.parse(string, timeFormatter);
            }
        });
        timeComboBox.getSelectionModel().select(LocalTime.of(12, 0)); // Default to noon
        
        // Initialize guests spinner (2-20 people)
        SpinnerValueFactory<Integer> valueFactory = 
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2);
        guestsSpinner.setValueFactory(valueFactory);
        
        // Set phone number if available from current user
        if (SimpleClient.getCurrentUserPhone() != null && !SimpleClient.getCurrentUserPhone().isEmpty()) {
            phoneField.setText(SimpleClient.getCurrentUserPhone());
        }
        
        // Disable reserve button until a table is selected
        reserveButton.setDisable(true);
        
        // Create initial empty grid layouts
        clearTableGrids();
        
        // Add real-time update listeners
        setupRealTimeUpdates();
        
        // Load initial tables to show at startup for better UX
        // Using default values (first branch, today, noon)
        Platform.runLater(() -> {
            String branchName = selectedBranchName;
            int branchId = selectedBranchId;
            LocalDate date = datePicker.getValue();
            LocalTime time = timeComboBox.getValue();
            
            System.out.println("🔵 TableDiagramController: Initializing tables for branch: " + branchName + " (ID: " + branchId + ")");
            System.out.println("🔵 TableDiagramController: Date: " + date + ", Time: " + time);
            createRealTables(branchId, date, time);
            statusLabel.setText("Select a table to make a reservation or adjust filters above");
        });
    }
    
    private void setupRealTimeUpdates() {
        // Listen for date changes
        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                System.out.println("Date changed to: " + newValue);
                updateTables();
            }
        });
        
        // Listen for time changes
        timeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                System.out.println("Time changed to: " + newValue);
                updateTables();
            }
        });
        
        // Listen for guest count changes
        guestsSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                System.out.println("Guest count changed to: " + newValue);
                updateTables();
            }
        });
    }
    
    private void updateTables() {
        // Reset selection state
        if (selectedTable != null) {
            selectedTable.setSelected(false);
            selectedTable = null;
        }
        selectedTableLabel.setText("No table selected");
        reserveButton.setDisable(true);
        
        // Get current values
        String branchName = selectedBranchName;
        LocalDate date = datePicker.getValue();
        LocalTime time = timeComboBox.getValue();
        
        // Make sure we have all required values
        if (branchName == null || date == null || time == null) {
            return;
        }
        
        // Clear and recreate tables
        clearTableGrids();
        
        int branchId = selectedBranchId;
        System.out.println("Real-time update - Creating tables for: " + branchName + 
                          " on " + date + " at " + time);
        
        createRealTables(branchId, date, time);
    }
    
    private void clearTableGrids() {
        barAreaGrid.getChildren().clear();
        insideAreaGrid.getChildren().clear();
        outsideAreaGrid.getChildren().clear();
    }
    
    private void createRealTables(int branchId, LocalDate date, LocalTime time) {
        int guestCount = guestsSpinner.getValue();
        
        System.out.println("🔵 TableDiagramController: Fetching real tables for " + guestCount + " guests on " + date + " at " + time);
        System.out.println("🔵 TableDiagramController: Branch ID: " + branchId);
        
        // Calculate the reservation end time (1.5 hours after selected time)
        LocalTime endTime = time.plusMinutes(90);
        LocalDateTime startDateTime = LocalDateTime.of(date, time);
        LocalDateTime endDateTime = LocalDateTime.of(date, endTime);
        
        System.out.println("🔵 TableDiagramController: Reservation window: " + startDateTime + " to " + endDateTime);
        
        try {
            // Fetch tables from server for this branch
            String message = "GET_BRANCH_TABLES;" + branchId;
            System.out.println("🔵 TableDiagramController: Sending message to server: " + message);
            SimpleClient.getClient().sendToServer(message);
            System.out.println("🔵 TableDiagramController: Message sent to server successfully");
            
        } catch (Exception e) {
            System.err.println("❌ TableDiagramController: Error requesting tables from server: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("❌ Error connecting to server");
        }
    }
    
    /**
     * Handle the response from server with real table data
     * This method will be called when we receive table data from the server
     */
    public void handleTablesResponse(List<RestaurantTable> tables) {
        System.out.println("Received " + tables.size() + " tables from server");
        
        // Clear existing table displays
        clearTableDisplays();
        
        if (tables.isEmpty()) {
            statusLabel.setText("❌ No tables found for this branch");
            return;
        }
        
        // Create simple table buttons instead of complex map
        createSimpleTableButtons(tables);
        
        statusLabel.setText("✅ Loaded " + tables.size() + " tables from database");
    }
    
    /**
     * Create table buttons positioned in the proper GridPane layout
     */
    private void createSimpleTableButtons(List<RestaurantTable> tables) {
        System.out.println("Creating " + tables.size() + " table buttons in GridPane layout...");
        
        // Show the GridPanes and clear them
        barAreaGrid.setVisible(true);
        insideAreaGrid.setVisible(true);
        outsideAreaGrid.setVisible(true);
        
        barAreaGrid.getChildren().clear();
        insideAreaGrid.getChildren().clear();
        outsideAreaGrid.getChildren().clear();
        
        // Position tables in the appropriate areas
        int insideRow = 0, insideCol = 0;
        int outsideRow = 0, outsideCol = 0;
        
        for (int i = 0; i < Math.min(tables.size(), 8); i++) {
            RestaurantTable table = tables.get(i);
            Button tableButton = createTableButton(table);
            
            // Position tables 1-4 in INSIDE AREA (2x2 grid)
            if (i < 4) {
                insideAreaGrid.add(tableButton, insideCol, insideRow);
                insideCol++;
                if (insideCol >= 2) {
                    insideCol = 0;
                    insideRow++;
                }
            }
            // Position tables 5-8 in OUTSIDE AREA (2x2 grid)
            else {
                outsideAreaGrid.add(tableButton, outsideCol, outsideRow);
                outsideCol++;
                if (outsideCol >= 2) {
                    outsideCol = 0;
                    outsideRow++;
                }
            }
            
            System.out.println("Positioned table " + table.getid() + " in " + (i < 4 ? "INSIDE" : "OUTSIDE") + " area");
        }
        
        statusLabel.setText("✅ Loaded " + tables.size() + " tables from database");
    }
    
    /**
     * Create a single table button with table information
     */
    private Button createTableButton(RestaurantTable table) {
        Button button = new Button();
        button.setPrefWidth(120);
        button.setPrefHeight(80);
        button.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        
        // Determine table status and styling
        boolean isReserved = table.getReservedID() > 0;
        String status = isReserved ? "RESERVED" : "AVAILABLE";
        String buttonStyle = isReserved ? 
            "-fx-background-color: #dc3545; -fx-text-fill: white;" : 
            "-fx-background-color: #28a745; -fx-text-fill: white;";
        
        button.setStyle(buttonStyle + " -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        
        // Set button text with table info (compact for smaller buttons)
        String buttonText = String.format("Table %d\n%d Seats\n%s", 
            table.getid(), 
            table.getSeatingCapacity(), 
            table.getLocation());
        button.setText(buttonText);
        
        // Add click handler
        button.setOnAction(e -> {
            if (!isReserved) {
                selectTable(table);
                button.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
                button.setText(buttonText + " | SELECTED");
            } else {
                statusLabel.setText("❌ This table is already reserved");
            }
        });
        
        return button;
    }
    
    /**
     * Clear all table displays
     */
    private void clearTableDisplays() {
        // Clear the map grids
        barAreaGrid.getChildren().clear();
        insideAreaGrid.getChildren().clear();
        outsideAreaGrid.getChildren().clear();
        
        // Reset selected table
        selectedTable = null;
        selectedTableLabel.setText("No table selected");
        reserveButton.setDisable(true);
    }
    
    /**
     * Select a table for reservation
     */
    private void selectTable(RestaurantTable table) {
        selectedTable = new TablePane("Table " + table.getid(), table.getSeatingCapacity(), table.getLocation(), false, table.getid());
        selectedTableLabel.setText("Selected: Table " + table.getid() + " (" + table.getSeatingCapacity() + " seats, " + table.getLocation() + ")");
        reserveButton.setDisable(false);
        
        System.out.println("Selected table: " + table.getid() + " with capacity " + table.getSeatingCapacity());
    }
    
    private void handleTableSelection(TablePane tablePane, int tableId, int branchId) {
        // Can't select unavailable tables
        if (!tablePane.isAvailable()) {
            statusLabel.setText("⚠️ This table is not available for the selected time.");
            return;
        }
        
        // Reset previous selection if exists
        if (selectedTable != null) {
            selectedTable.setSelected(false);
        }
        
        // Update new selection
        selectedTable = tablePane;
        tablePane.setSelected(true);
        
        // Update UI
        selectedTableLabel.setText("Selected: " + tablePane.getTableName() + " - " + 
                                   tablePane.getSeating() + " seats");
        reserveButton.setDisable(false);
        
        statusLabel.setText("✅ Table " + tablePane.getTableName() + " selected. Click 'Reserve Table' to confirm.");
    }
    
    @FXML
    private void handleReserveTable() {
        if (selectedTable == null) {
            statusLabel.setText("⚠️ Please select a table first.");
            return;
        }
        
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            statusLabel.setText("⚠️ Please enter your phone number.");
            return;
        }
        
        LocalDate date = datePicker.getValue();
        LocalTime time = timeComboBox.getValue();
        int guestCount = guestsSpinner.getValue();
        String branchName = selectedBranchName;
        int branchId = selectedBranchId;
        int tableId = selectedTable.getTableId();
        
        String location = selectedTable.getLocation();
        
        // Calculate end time (1.5 hours after start time)
        LocalTime endTime = time.plusMinutes(90);
        
        // Format: RESERVE_TABLE;branchId;guestCount;tableId;seatingPref;startDateTime;endDateTime;phoneNumber;location
        String reservationMessage = String.format("RESERVE_TABLE;%d;%d;%d;%s;%s;%s;%s;%s",
                branchId,
                guestCount,
                tableId,
                location,
                LocalDateTime.of(date, time).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                LocalDateTime.of(date, endTime).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                phone,
                location);
        
        statusLabel.setText("⏳ Sending reservation request...");
        
        try {
            System.out.println("Sending reservation request: " + reservationMessage);
            SimpleClient.getClient().sendToServer(reservationMessage);
        } catch (IOException e) {
            statusLabel.setText("❌ Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            cleanup();
            App.setRoot("reservation_branch_selection");
        } catch (IOException e) {
            statusLabel.setText("❌ Error returning to branch selection.");
            e.printStackTrace();
        }
    }
    
    @Subscribe
    public void onWarningEvent(WarningEvent event) {
        String message = event.getWarning().getMessage();
        
        Platform.runLater(() -> {
            if (message.startsWith("RESERVATION_SUCCESS")) {
                statusLabel.setText("🎉 Reservation confirmed!");
                // Add a delay before redirecting
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        Platform.runLater(() -> {
                            try {
                                cleanup();
                                App.setRoot("primary1");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else if (message.startsWith("RESERVATION_FAILURE")) {
                statusLabel.setText("❌ " + message.substring(message.indexOf(":") + 1).trim());
            }
        });
    }
    
    @Subscribe
    public void onTablesReceived(TablesReceivedEvent event) {
        List<RestaurantTable> tables = event.getTables();
        System.out.println("🔵 TableDiagramController: onTablesReceived called with " + tables.size() + " tables from server");
        
        Platform.runLater(() -> {
            System.out.println("🔵 TableDiagramController: Processing tables on JavaFX thread");
            handleTablesResponse(tables);
        });
    }
    
    public void cleanup() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
    
    // Custom component for table visualization
    private static class TablePane extends StackPane {
        private final Rectangle background;
        private final String tableName;
        private final int seating;
        private final String location;
        private final boolean available;
        private final int tableId;
        private boolean selected = false;
        
        public TablePane(String tableName, int seating, String location, boolean available) {
            this(tableName, seating, location, available, -1);
        }
        
        public TablePane(String tableName, int seating, String location, boolean available, int tableId) {
            this.tableName = tableName;
            this.seating = seating;
            this.location = location;
            this.available = available;
            this.tableId = tableId;
            
            // Create visual representation
            background = new Rectangle(100, 100);
            background.setFill(available ? Color.GREEN : Color.RED);
            background.setStroke(Color.BLACK);
            background.setStrokeWidth(2);
            background.setArcWidth(20);
            background.setArcHeight(20);
            
            // Create table label with improved visibility
            Text text = new Text(tableName + "\n" + seating + " seats");
            text.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            text.setFill(Color.BLACK);
            
            this.getChildren().addAll(background, text);
            this.setAlignment(Pos.CENTER);
            this.setMinSize(100, 100);
            this.setPrefSize(100, 100);
            
            // Add hover effect and cursor change
            if (available) {
                this.setCursor(javafx.scene.Cursor.HAND);
                this.setOnMouseEntered(e -> {
                    background.setFill(Color.LIGHTGREEN);
                    background.setStrokeWidth(3);
                    text.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
                });
                this.setOnMouseExited(e -> {
                    background.setFill(selected ? Color.LIGHTBLUE : Color.GREEN);
                    background.setStrokeWidth(2);
                    text.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                });
            }
        }
        
        public String getTableName() {
            return tableName;
        }
        
        public int getSeating() {
            return seating;
        }
        
        public String getLocation() {
            return location;
        }
        
        public boolean isAvailable() {
            return available;
        }
        
        public int getTableId() {
            return tableId;
        }
        
        public void setSelected(boolean selected) {
            this.selected = selected;
            if (available) {
                background.setFill(selected ? Color.LIGHTBLUE : Color.GREEN);
                background.setStrokeWidth(selected ? 3 : 2);
                if (selected) {
                    this.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLUE));
                } else {
                    this.setEffect(null);
                }
            }
        }
    }
} 