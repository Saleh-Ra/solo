package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationController {

    @FXML private ComboBox<String> branchComboBox;
    @FXML private TextField guestsField;
    @FXML private ComboBox<String> areaComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        // Sample values — you can load branches dynamically from DB later
        branchComboBox.getItems().addAll("Downtown", "Beachside", "Mountain View");
        areaComboBox.getItems().addAll("Inside", "Outside", "Bar", "Special Needs");
    }

    @FXML
    private void handleCheckAvailability() {
        String branch = branchComboBox.getValue();
        String guestsText = guestsField.getText();
        String area = areaComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String timeText = timeField.getText();

        if (branch == null || guestsText.isEmpty() || area == null || date == null || timeText.isEmpty()) {
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

        try {
            LocalTime.parse(timeText); // basic time format validation
        } catch (Exception e) {
            statusLabel.setText("⚠ Invalid time format (use HH:mm).");
            return;
        }

        // TODO: Here we would check availability against server/database
        System.out.printf("Checking availability: %s, %d guests, %s at %s %s%n",
                branch, guests, area, date, timeText);

        // Placeholder message
        statusLabel.setText("✅ Reservation slot appears available! (To be verified with DB)");
    }

    @FXML
    private void handleBackToMain() {
        try {
            App.setRoot("primary1");
        } catch (IOException e) {
            statusLabel.setText("❌ Could not return to main screen.");
            e.printStackTrace();
        }
    }
}
