package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.client.WarningEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.RestaurantTable;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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

public class TableDiagramController {

    @FXML private ComboBox<String> branchComboBox;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<LocalTime> timeComboBox;
    @FXML private Spinner<Integer> guestsSpinner;
    @FXML private TextField phoneField;
    @FXML private Button reserveButton;
    @FXML private Label statusLabel;
    @FXML private Label selectedTableLabel;
    
    @FXML private GridPane barAreaGrid;
    @FXML private GridPane insideAreaGrid;
    @FXML private GridPane outsideAreaGrid;
    
    // Maps branch names to their IDs
    private final Map<String, Integer> branchIdMap = new HashMap<>();
    // Store the currently selected table reference
    private TablePane selectedTable = null;
    
    // Branch names (matched with IDs in database)
    private final String[] branchNames = {"Tel-Aviv", "Haifa", "Jerusalem"};
    
    @FXML
    public void initialize() {
        // Register with EventBus
        EventBus.getDefault().register(this);
        
        // Initialize branch combo box
        branchComboBox.getItems().addAll(branchNames);
        for (int i = 0; i < branchNames.length; i++) {
            branchIdMap.put(branchNames[i], i + 1); // Branch IDs are 1-based
        }
        branchComboBox.getSelectionModel().selectFirst();
        
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
            String branchName = branchComboBox.getValue();
            int branchId = branchIdMap.get(branchName);
            LocalDate date = datePicker.getValue();
            LocalTime time = timeComboBox.getValue();
            
            System.out.println("Initializing tables for branch: " + branchName);
            createSampleTables(branchId, date, time);
            statusLabel.setText("Select a table to make a reservation or adjust filters above");
        });
    }
    
    private void setupRealTimeUpdates() {
        // Listen for branch changes
        branchComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                System.out.println("Branch changed to: " + newValue);
                updateTables();
            }
        });
        
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
        String branchName = branchComboBox.getValue();
        LocalDate date = datePicker.getValue();
        LocalTime time = timeComboBox.getValue();
        
        // Make sure we have all required values
        if (branchName == null || date == null || time == null) {
            return;
        }
        
        // Clear and recreate tables
        clearTableGrids();
        
        int branchId = branchIdMap.get(branchName);
        System.out.println("Real-time update - Creating tables for: " + branchName + 
                          " on " + date + " at " + time);
        
        createSampleTables(branchId, date, time);
    }
    
    private void clearTableGrids() {
        barAreaGrid.getChildren().clear();
        insideAreaGrid.getChildren().clear();
        outsideAreaGrid.getChildren().clear();
    }
    
    private void createSampleTables(int branchId, LocalDate date, LocalTime time) {
        int guestCount = guestsSpinner.getValue();
        
        System.out.println("Creating tables for " + guestCount + " guests on " + date + " at " + time);
        
        // In a real implementation, we would fetch actual table data from the server
        // along with any reservations for the selected date and time
        
        // Calculate the reservation end time (1.5 hours after selected time)
        LocalTime endTime = time.plusMinutes(90);
        LocalDateTime startDateTime = LocalDateTime.of(date, time);
        LocalDateTime endDateTime = LocalDateTime.of(date, endTime);
        
        System.out.println("Reservation window: " + startDateTime + " to " + endDateTime);
        
        System.out.println("Creating bar area tables...");
        // Bar area (smaller tables, 2-4 seats)
        for (int i = 0; i < 4; i++) {
            int capacity = 2 + (i % 3);
            int tableId = i + 1;
            
            // In a real implementation, we would check if this table has any reservations
            // that overlap with the selected time window
            boolean isReserved = isTableReserved(branchId, tableId, startDateTime, endDateTime);
            
            // Table is available if it has enough seats and isn't already reserved
            boolean isAvailable = capacity >= guestCount && !isReserved;
            
            TablePane tablePane = new TablePane("Bar " + (i+1), capacity, "Bar", isAvailable, tableId);
            
            // Add click event handler for table selection
            final int finalTableId = tableId;
            tablePane.setOnMouseClicked(event -> handleTableSelection(tablePane, finalTableId, branchId));
            
            barAreaGrid.add(tablePane, i % 2, i / 2);
            System.out.println("Added Bar table " + (i+1) + " (capacity: " + capacity + ") at position (" + 
                              (i % 2) + "," + (i / 2) + "), available: " + isAvailable + ", id: " + tableId);
        }
        
        System.out.println("Creating inside area tables...");
        // Inside area (medium tables, 4-6 seats)
        for (int i = 0; i < 12; i++) {
            int capacity = 4 + (i % 3);
            int tableId = i + 5; // Continuing the IDs
            
            // Check if table is reserved at the selected time
            boolean isReserved = isTableReserved(branchId, tableId, startDateTime, endDateTime);
            
            // Table is available if it has enough seats and isn't already reserved
            boolean isAvailable = capacity >= guestCount && !isReserved;
            
            TablePane tablePane = new TablePane("Inside " + (i+1), capacity, "Inside", isAvailable, tableId);
            
            final int finalTableId = tableId;
            tablePane.setOnMouseClicked(event -> handleTableSelection(tablePane, finalTableId, branchId));
            
            insideAreaGrid.add(tablePane, i % 4, i / 4);
            System.out.println("Added Inside table " + (i+1) + " (capacity: " + capacity + ") at position (" + 
                              (i % 4) + "," + (i / 4) + "), available: " + isAvailable + ", id: " + tableId);
        }
        
        System.out.println("Creating outside area tables...");
        // Outside area (varied tables, 2-8 seats)
        for (int i = 0; i < 8; i++) {
            int capacity = 2 + (i % 7);
            int tableId = i + 17; // Continuing the IDs
            
            // Check if table is reserved at the selected time
            boolean isReserved = isTableReserved(branchId, tableId, startDateTime, endDateTime);
            
            // Table is available if it has enough seats and isn't already reserved
            boolean isAvailable = capacity >= guestCount && !isReserved;
            
            TablePane tablePane = new TablePane("Outside " + (i+1), capacity, "Outside", isAvailable, tableId);
            
            final int finalTableId = tableId;
            tablePane.setOnMouseClicked(event -> handleTableSelection(tablePane, finalTableId, branchId));
            
            outsideAreaGrid.add(tablePane, i % 3, i / 3);
            System.out.println("Added Outside table " + (i+1) + " (capacity: " + capacity + ") at position (" + 
                              (i % 3) + "," + (i / 3) + "), available: " + isAvailable + ", id: " + tableId);
        }
        
        System.out.println("Table creation complete. Bar: 4, Inside: 12, Outside: 8 tables");
        statusLabel.setText("✅ Table availability updated. Select an available table.");
    }
    
    /**
     * Check if a table is reserved during the specified time window.
     * In a real implementation, this would query the database.
     * For now, we'll use a simple deterministic algorithm for demo purposes.
     */
    private boolean isTableReserved(int branchId, int tableId, LocalDateTime start, LocalDateTime end) {
        // For demo purposes: predictable "reservations" based on deterministic factors
        // In a real implementation, this would query the server for actual reservations
        
        // Tables with even IDs are reserved on even dates, odd IDs on odd dates
        if (tableId % 2 == 0 && start.getDayOfMonth() % 2 == 0) {
            // Reserved between 11 AM and 2 PM, and 6 PM and 9 PM
            LocalTime tableTime = start.toLocalTime();
            return (tableTime.isAfter(LocalTime.of(11, 0)) && tableTime.isBefore(LocalTime.of(14, 0))) ||
                   (tableTime.isAfter(LocalTime.of(18, 0)) && tableTime.isBefore(LocalTime.of(21, 0)));
        } else if (tableId % 2 == 1 && start.getDayOfMonth() % 2 == 1) {
            // Reserved between 12 PM and 3 PM, and 7 PM and 10 PM
            LocalTime tableTime = start.toLocalTime();
            return (tableTime.isAfter(LocalTime.of(12, 0)) && tableTime.isBefore(LocalTime.of(15, 0))) ||
                   (tableTime.isAfter(LocalTime.of(19, 0)) && tableTime.isBefore(LocalTime.of(22, 0)));
        }
        
        // Most tables are available by default
        return false;
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
        String branchName = branchComboBox.getValue();
        int branchId = branchIdMap.get(branchName);
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
            App.setRoot("primary1");
        } catch (IOException e) {
            statusLabel.setText("❌ Error returning to main screen.");
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