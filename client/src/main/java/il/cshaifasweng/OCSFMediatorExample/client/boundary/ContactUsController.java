package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ContactUsController {

    @FXML private ComboBox<String> complaintCategoryComboBox;
    @FXML private TextArea complaintMessageArea;
    @FXML private Button submitComplaintButton;
    @FXML private VBox pastComplaintsBox;
    @FXML private TextArea serverMessagesArea;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        complaintCategoryComboBox.getItems().addAll(
                "Refund Issue", "Late Delivery", "Wrong Order",
                "Reservation Problem", "Bad Experience", "Other"
        );
        complaintCategoryComboBox.getSelectionModel().selectFirst();

        // Load existing complaints (demo for now)
        addComplaintHistory("Late Delivery", "Pending", LocalDateTime.now().minusDays(2));
        addComplaintHistory("Wrong Order", "Resolved", LocalDateTime.now().minusDays(1));

        // Load server messages (example)
        addServerMessage("Your refund has been approved for Order #1234", LocalDateTime.now().minusHours(3));
        addServerMessage("We apologize for the inconvenience.", LocalDateTime.now().minusHours(2));
    }

    @FXML
    private void handleSubmitComplaint() {
        String category = complaintCategoryComboBox.getValue();
        String message = complaintMessageArea.getText().trim();

        if (message.isEmpty()) {
            showAlert("Please enter a message for your complaint.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        addComplaintHistory(category, "Pending", now);
        addServerMessage("Complaint submitted: " + category, now);
        complaintMessageArea.clear();
    }

    private void addComplaintHistory(String category, String status, LocalDateTime date) {
        String label = String.format("📌 %s  |  Status: %s  |  %s", category, status, date.format(formatter));
        pastComplaintsBox.getChildren().add(new Label(label));
    }

    private void addServerMessage(String text, LocalDateTime timestamp) {
        String formatted = String.format("[%s] %s", timestamp.format(formatter), text);
        serverMessagesArea.appendText(formatted + "\n");
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("personal_area");
        } catch (IOException e) {
            showAlert("Failed to return to personal area.");
        }
    }
} 