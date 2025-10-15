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
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TablesMapController {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<LocalTime> timeComboBox;
    @FXML private Label statusLabel;
    @FXML private Label selectedTableLabel;
    @FXML private Label branchLabel;
    @FXML private VBox dynamicTableContainer;
    
    // Store table data and selection
    private List<RestaurantTable> tableList = new ArrayList<>();
    private RestaurantTable selectedTable = null;
    private Map<Integer, Button> tableButtons = new HashMap<>();
    private java.util.Set<Integer> occupiedTableIds = new java.util.HashSet<>();
    private int selectedBranchId;
    private String selectedBranchName;
    
    @FXML
    public void initialize() {
        // Get selected branch
        selectedBranchId = SimpleClient.getSelectedBranchId();
        selectedBranchName = SimpleClient.getSelectedBranchName();
        
        if (selectedBranchId == 0 || selectedBranchName == null) {
            try {
                App.setRoot("reservation_branch_selection");
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        
        // Register with EventBus
        System.out.println("🔵 TablesMapController: Registering with EventBus");
        EventBus.getDefault().register(this);
        System.out.println("🔵 TablesMapController: Registered with EventBus");
        
        // Setup UI
        setupUI();
        
        // Load tables with availability for the initial date/time selection
        loadTablesWithAvailability();
        
        // Add listeners for date/time changes
        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && timeComboBox.getValue() != null) {
                System.out.println("🔵 Date changed to: " + newValue + ", updating colors...");
                updateTableColorsForTime();
            }
        });
        
        timeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && datePicker.getValue() != null) {
                System.out.println("🔵 Time changed to: " + newValue + ", updating colors...");
                updateTableColorsForTime();
            }
        });
    }
    
    private void setupUI() {
        // Set branch label
        branchLabel.setText("📍 " + selectedBranchName + " Branch");
        
        // Initialize date picker to today
        datePicker.setValue(LocalDate.now());
        
        // Setup time combo box
        List<LocalTime> timeSlots = new ArrayList<>();
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(23, 0);
        
        while (start.isBefore(end)) {
            timeSlots.add(start);
            start = start.plusMinutes(15);
        }
        
        timeComboBox.setItems(FXCollections.observableArrayList(timeSlots));
        timeComboBox.setConverter(new StringConverter<>() {
            private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            @Override
            public String toString(LocalTime time) {
                return time != null ? timeFormatter.format(time) : "";
            }
            
            @Override
            public LocalTime fromString(String string) {
                return string != null && !string.isEmpty() ? LocalTime.parse(string, timeFormatter) : null;
            }
        });
        timeComboBox.getSelectionModel().select(LocalTime.of(12, 0));
        
        // No need for guests spinner or phone field in table management view
        
        // Setup table button click handlers
        setupTableButtons();
    }
    
    private void setupTableButtons() {
        // Clear existing dynamic table container
        dynamicTableContainer.getChildren().clear();
        tableButtons.clear();
        
        // Add loading message
        Label loadingLabel = new Label("Loading tables...");
        loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6c757d;");
        dynamicTableContainer.getChildren().add(loadingLabel);
    }
    
    private void loadTablesWithAvailability() {
        try {
            // First, get the basic table info
            String message = "GET_BRANCH_TABLES;" + selectedBranchId;
            System.out.println("🔵 TablesMapController: Sending basic table request: " + message);
            SimpleClient.getClient().sendToServer(message);
            System.out.println("🔵 TablesMapController: Basic table request sent for branch: " + selectedBranchId);
            statusLabel.setText("⏳ Loading tables...");
        } catch (Exception e) {
            System.err.println("❌ TablesMapController: Error requesting tables: " + e.getMessage());
            statusLabel.setText("❌ Error connecting to server");
        }
    }
    
    private void updateTableColorsForTime() {
        if (tableList.isEmpty()) {
            return;
        }
        
        LocalDate date = datePicker.getValue();
        LocalTime time = timeComboBox.getValue();
        
        if (date == null || time == null) {
            return;
        }
        
        // Request reservations for this specific date/time
        LocalDateTime selectedDateTime = LocalDateTime.of(date, time);
        String dateTimeStr = selectedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String message = "GET_RESERVATIONS_FOR_TIME;" + selectedBranchId + ";" + dateTimeStr;
        
        try {
            System.out.println("🔵 Client sending time: " + selectedDateTime);
            System.out.println("🔵 Client sending string: " + dateTimeStr);
            SimpleClient.getClient().sendToServer(message);
            statusLabel.setText("⏳ Checking availability for " + date + " at " + time);
        } catch (Exception e) {
            System.err.println("❌ Error checking reservations: " + e.getMessage());
        }
    }
    
    
    @Subscribe
    public void onTablesReceived(SimpleClient.TablesReceivedEvent event) {
        System.out.println("🟢 TablesMapController: onTablesReceived called with " + event.getTables().size() + " tables");
        Platform.runLater(() -> {
            tableList = event.getTables();
            System.out.println("🟢 TablesMapController: Processing " + tableList.size() + " tables from server");
            updateTableButtons();
            // After loading tables, check availability for current time
            updateTableColorsForTime();
        });
    }
    
    @Subscribe
    public void onReservationsReceived(SimpleClient.ReservationsReceivedEvent event) {
        System.out.println("🟢 TablesMapController: onReservationsReceived called with " + event.getReservations().size() + " reservations");
        Platform.runLater(() -> {
            // Update table colors based on reservations
            updateTableColors(event.getReservations());
            statusLabel.setText("✅ Updated availability status");
        });
    }
    
    private void updateTableColors(List<il.cshaifasweng.OCSFMediatorExample.entities.Reservation> reservations) {
        if (tableList.isEmpty()) {
            return;
        }
        
        LocalDate selectedDate = datePicker.getValue();
        LocalTime selectedTime = timeComboBox.getValue();
        
        if (selectedDate == null || selectedTime == null) {
            return;
        }
        
        LocalDateTime selectedDateTime = LocalDateTime.of(selectedDate, selectedTime);
        
        // Update each button color based on reservations
        for (RestaurantTable table : tableList) {
            Button button = tableButtons.get(table.getid());
            if (button == null) continue;
            
            boolean isReserved = false;
            
            // Check if this table has a reservation at the selected time
            for (il.cshaifasweng.OCSFMediatorExample.entities.Reservation reservation : reservations) {
                if (reservation.getTables() != null) {
                    for (RestaurantTable reservedTable : reservation.getTables()) {
                        if (reservedTable.getid() == table.getid()) {
                            // Check if selected time is within the reservation window
                            // Table is reserved from reservation start time until 90 minutes after
                            LocalDateTime resStart = reservation.getReservationTime();
                            LocalDateTime resEnd = reservation.getEndTime(); // Should be resStart + 90 minutes
                            
                            // Selected time is within reservation if: resStart <= selectedDateTime < resEnd
                            if (!selectedDateTime.isBefore(resStart) && selectedDateTime.isBefore(resEnd)) {
                                isReserved = true;
                                break;
                            }
                        }
                    }
                }
                if (isReserved) break;
            }
            
            // Set button color
            if (isReserved) {
                button.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                System.out.println("🔴 Table " + table.getid() + " marked as RED (reserved)");
            } else {
                button.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                System.out.println("🟢 Table " + table.getid() + " marked as GREEN (available)");
            }
        }
    }
    
    private void updateTableButtons() {
        if (tableList.isEmpty()) {
            statusLabel.setText("❌ No table data received");
            return;
        }
        
        // Clear existing container
        dynamicTableContainer.getChildren().clear();
        tableButtons.clear();
        
        System.out.println("🔵 Creating " + tableList.size() + " dynamic table buttons");
        
        // Group tables by location
        Map<String, List<RestaurantTable>> tablesByLocation = new HashMap<>();
        for (RestaurantTable table : tableList) {
            String location = table.getLocation() != null ? table.getLocation() : "Unknown";
            tablesByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(table);
        }
        
        // Create sections for each location
        for (Map.Entry<String, List<RestaurantTable>> entry : tablesByLocation.entrySet()) {
            String location = entry.getKey();
            List<RestaurantTable> tables = entry.getValue();
            
            // Create location label
            Label locationLabel = new Label(location.toUpperCase() + " AREA");
            locationLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #495057;");
            dynamicTableContainer.getChildren().add(locationLabel);
            
            // Create grid for tables in this location
            GridPane gridPane = new GridPane();
            gridPane.setAlignment(Pos.CENTER);
            gridPane.setHgap(15);
            gridPane.setVgap(15);
            
            // Add tables to grid (max 4 per row)
            int colsPerRow = 4;
            for (int i = 0; i < tables.size(); i++) {
                RestaurantTable table = tables.get(i);
                int row = i / colsPerRow;
                int col = i % colsPerRow;
                
                Button button = createTableButton(table);
                gridPane.add(button, col, row);
                tableButtons.put(table.getid(), button);
            }
            
            dynamicTableContainer.getChildren().add(gridPane);
        }
        
        statusLabel.setText("✅ Loaded " + tableList.size() + " tables. Checking availability...");
    }
    
    private Button createTableButton(RestaurantTable table) {
        Button button = new Button();
        button.setPrefWidth(90);
        button.setPrefHeight(70);
        
        // Set button text
        String buttonText = String.format("Table %d\n%d Seats", 
            table.getid(), 
            table.getSeatingCapacity());
        button.setText(buttonText);
        
        // Set default button style (will be updated when we get reservation data)
        button.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
        button.setDisable(false);
        
        // Add click handler
        button.setOnAction(e -> selectTable(table, button));
        
        System.out.println("⚪ Table " + table.getid() + " created in " + table.getLocation() + " area");
        
        return button;
    }
    
    private void selectTable(RestaurantTable table, Button button) {
        // Reset all buttons to their original state
        resetAllButtons();
        
        // Highlight selected button
        button.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        // Store selection
        selectedTable = table;
        selectedTableLabel.setText("Selected: Table " + table.getid() + " (" + table.getSeatingCapacity() + " seats)");
        
        System.out.println("Selected table: " + table.getid());
    }
    
    private void resetAllButtons() {
        // Reset all dynamic buttons to their original status colors
        for (RestaurantTable table : tableList) {
            Button button = tableButtons.get(table.getid());
            if (button == null) continue;
            
            // Use reservation status to determine color
            boolean isOccupied = occupiedTableIds.contains(table.getid());
            if (isOccupied) {
                button.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                button.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        }
    }
    
    // No reservation functionality - this is now a table management view only
    
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
                System.out.println("🎉 Reservation successful - updating table display...");
                
                // Reload tables with availability to show updated status
                loadTablesWithAvailability();
                
                // Add a delay before redirecting
                new Thread(() -> {
                    try {
                        Thread.sleep(3000); // Wait to see the update
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
    
    /**
     * Manual refresh method - can be called to refresh table display
     */
    public void refreshTableDisplay() {
        System.out.println("🔵 Manual refresh requested - reloading tables...");
        loadTablesWithAvailability();
    }
}
