package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.client.WarningEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ReservationController {

    @FXML private ComboBox<String> branchComboBox;
    @FXML private TextField guestsField;
    @FXML private ComboBox<String> areaComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField phoneField; // Added phone field for client identification
    @FXML private Label statusLabel;
    
    // Maps for branch IDs (we'll use position as ID for simplicity)
    private final String[] branchNames = {"Tel-Aviv", "Haifa", "Jerusalem", "Beer-Sheva"};

    @FXML
    public void initialize() {
        // Register with EventBus to receive server responses
        EventBus.getDefault().register(this);
        
        // Add branches with IDs (position + 1)
        branchComboBox.getItems().addAll(branchNames);
        areaComboBox.getItems().addAll("Inside", "Outside", "Bar", "Special Needs");
    }
    
    // Handle server responses
    @Subscribe
    public void onWarningEvent(WarningEvent event) {
        String message = event.getWarning().getMessage();
        
        if (message.startsWith("RESERVATION_SUCCESS")) {
            // Reservation was successful
            statusLabel.setText("✅ Reservation confirmed!");
            // Add a delay before redirecting to the main page
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> {
                        try {
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
            // Reservation failed
            statusLabel.setText("❌ " + message.substring(message.indexOf(":") + 1).trim());
        }
    }

    @FXML
    private void handleCheckAvailability() {
        String branchName = branchComboBox.getValue();
        String guestsText = guestsField.getText();
        String area = areaComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String timeText = timeField.getText();
        String phone = phoneField.getText().trim();

        if (branchName == null || guestsText.isEmpty() || area == null || date == null || timeText.isEmpty() || phone.isEmpty()) {
            statusLabel.setText("⚠ Please fill in all fields.");
            return;
        }

        int guests;
        try {
            guests = Integer.parseInt(guestsText);
            if (guests < 1) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("⚠ Number of guests must be a valid positive number.");
            return;
        }
        
        LocalTime time;
        try {
            time = LocalTime.parse(timeText);
        } catch (Exception e) {
            statusLabel.setText("⚠ Invalid time format (use HH:mm).");
            return;
        }
        
        // Validate phone number
        if (phone.length() < 10) {
            statusLabel.setText("⚠ Please enter a valid phone number.");
            return;
        }

        // Convert date and time to LocalDateTime for the reservation
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        
        // Calculate end time (1.5 hours after start time, as per Reservation entity)
        LocalDateTime endDateTime = dateTime.plusMinutes(90);
        
        // Get branch ID (position + 1 as simple mapping)
        int branchId = getBranchId(branchName);
        
        // Format: RESERVE_TABLE;branchId;guestCount;area;startDateTime;endDateTime;phoneNumber;location
        String reservationMessage = String.format("RESERVE_TABLE;%d;%d;%s;%s;%s;%s",
                branchId,
                guests,
                area,
                dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                endDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                phone);
                
        statusLabel.setText("⏳ Sending reservation request...");
        
        try {
            System.out.println("Sending reservation request: " + reservationMessage);
            SimpleClient.getClient().sendToServer(reservationMessage);
        } catch (IOException e) {
            statusLabel.setText("❌ Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private int getBranchId(String branchName) {
        // Simple mapping - position in array + 1
        for (int i = 0; i < branchNames.length; i++) {
            if (branchNames[i].equals(branchName)) {
                return i + 1; // Use 1-based indexing for IDs
            }
        }
        return 1; // Default to first branch if not found
    }

    @FXML
    private void handleBackToMain() {
        // Unregister from EventBus
        EventBus.getDefault().unregister(this);
        
        try {
            App.setRoot("primary1");
        } catch (IOException e) {
            statusLabel.setText("❌ Could not return to main screen.");
            e.printStackTrace();
        }
    }
    
    // Clean up when controller is being destroyed
    public void cleanup() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
}
