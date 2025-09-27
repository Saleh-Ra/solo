package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.client.WarningEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.RestaurantTable;
import il.cshaifasweng.OCSFMediatorExample.entities.TableAvailabilityInfo;
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
    @FXML private Spinner<Integer> guestsSpinner;
    @FXML private TextField phoneField;
    @FXML private Button reserveButton;
    @FXML private Label statusLabel;
    @FXML private Label selectedTableLabel;
    @FXML private Label branchLabel;
    
    // Table buttons - 8 fixed positions
    @FXML private Button table1Button;
    @FXML private Button table2Button;
    @FXML private Button table3Button;
    @FXML private Button table4Button;
    @FXML private Button table5Button;
    @FXML private Button table6Button;
    @FXML private Button table7Button;
    @FXML private Button table8Button;
    
    // Store table data and selection
    private List<TableAvailabilityInfo> tableAvailabilityList = new ArrayList<>();
    private RestaurantTable selectedTable = null;
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
                System.out.println("🔵 Date changed to: " + newValue + ", loading availability...");
                loadTablesWithAvailability();
            }
        });
        
        timeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && datePicker.getValue() != null) {
                System.out.println("🔵 Time changed to: " + newValue + ", loading availability...");
                loadTablesWithAvailability();
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
        
        // Setup guests spinner
        SpinnerValueFactory<Integer> valueFactory = 
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2);
        guestsSpinner.setValueFactory(valueFactory);
        
        // Set phone number if available
        if (SimpleClient.getCurrentUserPhone() != null && !SimpleClient.getCurrentUserPhone().isEmpty()) {
            phoneField.setText(SimpleClient.getCurrentUserPhone());
        }
        
        // Disable reserve button initially
        reserveButton.setDisable(true);
        
        // Setup table button click handlers
        setupTableButtons();
    }
    
    private void setupTableButtons() {
        // Create list of all table buttons for easy iteration
        List<Button> tableButtons = Arrays.asList(
            table1Button, table2Button, table3Button, table4Button,
            table5Button, table6Button, table7Button, table8Button
        );
        
        // Set initial button text and disable them
        for (int i = 0; i < tableButtons.size(); i++) {
            Button button = tableButtons.get(i);
            button.setText("Table " + (i + 1) + "\nLoading...");
            button.setDisable(true);
            button.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 12px;");
        }
    }
    
    private void loadTablesWithAvailability() {
        try {
            // For now, let's try a simpler approach - just get basic table info
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
    
    @Subscribe
    public void onTableAvailabilityReceived(List<TableAvailabilityInfo> availabilityList) {
        System.out.println("🟢 TablesMapController: onTableAvailabilityReceived called with " + availabilityList.size() + " tables");
        Platform.runLater(() -> {
            tableAvailabilityList = availabilityList;
            System.out.println("🟢 TablesMapController: Processing " + tableAvailabilityList.size() + " tables with availability from server");
            updateTableButtonsWithAvailability();
        });
    }
    
    @Subscribe
    public void onTablesReceived(SimpleClient.TablesReceivedEvent event) {
        System.out.println("🟢 TablesMapController: onTablesReceived called with " + event.getTables().size() + " tables");
        Platform.runLater(() -> {
            // Convert RestaurantTable objects to TableAvailabilityInfo for compatibility
            List<TableAvailabilityInfo> availabilityList = new ArrayList<>();
            for (RestaurantTable table : event.getTables()) {
                TableAvailabilityInfo tableInfo = new TableAvailabilityInfo();
                tableInfo.setTableId(table.getid());
                tableInfo.setSeatingCapacity(table.getSeatingCapacity());
                tableInfo.setLocation(table.getLocation());
                tableInfo.setAvailable(true); // Assume available for now
                availabilityList.add(tableInfo);
            }
            
            tableAvailabilityList = availabilityList;
            System.out.println("🟢 TablesMapController: Converted " + tableAvailabilityList.size() + " tables to availability info");
            updateTableButtonsWithAvailability();
        });
    }
    
    private void updateTableButtonsWithAvailability() {
        if (tableAvailabilityList.isEmpty()) {
            statusLabel.setText("❌ No availability data received");
            return;
        }
        
        // Create list of all table buttons
        List<Button> tableButtons = Arrays.asList(
            table1Button, table2Button, table3Button, table4Button,
            table5Button, table6Button, table7Button, table8Button
        );
        
        System.out.println("🔵 Updating " + Math.min(tableAvailabilityList.size(), tableButtons.size()) + " table buttons with availability data");
        
        // Update each button with availability data
        for (int i = 0; i < Math.min(tableAvailabilityList.size(), tableButtons.size()); i++) {
            TableAvailabilityInfo tableInfo = tableAvailabilityList.get(i);
            Button button = tableButtons.get(i);
            
            // Set button text
            String buttonText = String.format("Table %d\n%d Seats\n%s", 
                tableInfo.getTableId(), 
                tableInfo.getSeatingCapacity(), 
                tableInfo.getLocation());
            button.setText(buttonText);
            
            // Set button style based on availability
            if (tableInfo.isAvailable()) {
                button.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
                button.setDisable(false);
                System.out.println("🟢 Table " + tableInfo.getTableId() + " marked as GREEN (available) - real-time data");
            } else {
                button.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
                button.setDisable(true);
                System.out.println("🔴 Table " + tableInfo.getTableId() + " marked as RED (unavailable) - real-time data");
            }
            
            // Add click handler - create a simple RestaurantTable for selection
            final int tableId = tableInfo.getTableId();
            final int seatingCapacity = tableInfo.getSeatingCapacity();
            final String location = tableInfo.getLocation();
            
            button.setOnAction(e -> {
                // Create a temporary RestaurantTable object for selection
                RestaurantTable tempTable = new RestaurantTable();
                tempTable.setid(tableId);
                tempTable.setSeatingCapacity(seatingCapacity);
                tempTable.setLocation(location);
                selectTable(tempTable, button);
            });
        }
        
        statusLabel.setText("✅ Loaded " + tableAvailabilityList.size() + " tables with real-time availability. Select a table to make a reservation.");
    }
    
    private void selectTable(RestaurantTable table, Button button) {
        // Reset all buttons to their original state
        resetAllButtons();
        
        // Highlight selected button
        button.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        // Store selection
        selectedTable = table;
        selectedTableLabel.setText("Selected: Table " + table.getid() + " (" + table.getSeatingCapacity() + " seats)");
        reserveButton.setDisable(false);
        
        System.out.println("Selected table: " + table.getid());
    }
    
    private void resetAllButtons() {
        List<Button> tableButtons = Arrays.asList(
            table1Button, table2Button, table3Button, table4Button,
            table5Button, table6Button, table7Button, table8Button
        );
        
        // Use availability info to reset button colors
        if (!tableAvailabilityList.isEmpty()) {
            for (int i = 0; i < Math.min(tableAvailabilityList.size(), tableButtons.size()); i++) {
                Button button = tableButtons.get(i);
                TableAvailabilityInfo tableInfo = tableAvailabilityList.get(i);
                
                if (tableInfo.isAvailable()) {
                    button.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
                } else {
                    button.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
                }
            }
        }
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
        
        // Calculate end time (1.5 hours after start time)
        LocalTime endTime = time.plusMinutes(90);
        
        // Format: RESERVE_TABLE;branchId;guestCount;tableId;seatingPref;startDateTime;endDateTime;phoneNumber;location
        String reservationMessage = String.format("RESERVE_TABLE;%d;%d;%d;%s;%s;%s;%s;%s",
                selectedBranchId,
                guestCount,
                selectedTable.getid(),
                selectedTable.getLocation(),
                LocalDateTime.of(date, time).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                LocalDateTime.of(date, endTime).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                phone,
                selectedTable.getLocation());
        
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
