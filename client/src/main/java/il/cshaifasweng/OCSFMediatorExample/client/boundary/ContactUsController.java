package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.client.WarningEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
import il.cshaifasweng.OCSFMediatorExample.entities.GeneralMessage;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;

public class ContactUsController {

    @FXML private ComboBox<String> messageTypeComboBox;
    @FXML private HBox branchHBox;
    @FXML private ComboBox<String> branchComboBox;
    @FXML private Label categoryLabel;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextArea messageArea;
    @FXML private Button submitButton;
    @FXML private VBox pastMessagesBox;
    @FXML private Label historyLabel;
    @FXML private Label statusLabel;

    private final String[] branchNames = {"Tel-Aviv", "Haifa", "Jerusalem", "Beer-Sheva"};

    @FXML
    public void initialize() {
        // Register with EventBus to receive server responses
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        // Setup message type selection
        messageTypeComboBox.getItems().addAll("General", "Complaint");
        messageTypeComboBox.getSelectionModel().select("Complaint");

        // Setup branch selection
        branchComboBox.getItems().addAll(branchNames);
        int selectedBranchId = SimpleClient.getSelectedBranchId();
        if (selectedBranchId > 0 && selectedBranchId <= branchNames.length) {
            branchComboBox.getSelectionModel().select(selectedBranchId - 1);
        } else {
            branchComboBox.getSelectionModel().selectFirst();
        }

        // Setup initial categories (complaint categories)
        updateCategoriesForMessageType("Complaint");

        // Load past messages from server
        loadPastMessages();
    }

    @FXML
    private void handleMessageTypeChanged() {
        String selectedType = messageTypeComboBox.getValue();
        if (selectedType == null) return;
        
        updateCategoriesForMessageType(selectedType);
        
        // Show/hide branch selection based on message type
        if ("General".equals(selectedType)) {
            branchHBox.setVisible(false);
            branchHBox.setManaged(false);
            categoryLabel.setText("Category:");
            submitButton.setText("Send Message");
            historyLabel.setText("📄 Past Messages");
        } else {
            branchHBox.setVisible(true);
            branchHBox.setManaged(true);
            categoryLabel.setText("Complaint Type:");
            submitButton.setText("Submit Complaint");
            historyLabel.setText("📄 Past Complaints");
        }
    }

    private void updateCategoriesForMessageType(String messageType) {
        categoryComboBox.getItems().clear();
        if ("General".equals(messageType)) {
            categoryComboBox.getItems().addAll(
                    "Suggestion", "Feedback", "Question", "Improvement Idea", "Other"
            );
        } else {
            categoryComboBox.getItems().addAll(
                    "Refund Issue", "Late Delivery", "Wrong Order",
                    "Reservation Problem", "Bad Experience", "Other"
            );
        }
        categoryComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSubmit() {
        String messageType = messageTypeComboBox.getValue();
        String category = categoryComboBox.getValue();
        String message = messageArea.getText().trim();

        // Validation
        if (messageType == null) {
            showAlert("Please select a message type.");
            return;
        }
        if (category == null) {
            showAlert("Please select a category.");
            return;
        }
        if (message.isEmpty()) {
            showAlert("Please enter a message.");
            return;
        }

        // Get phone number
        String phoneNumber = SimpleClient.getCurrentUserPhone();
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            showAlert("You must be logged in to send a message. Please sign in first.");
            return;
        }

        try {
            if ("Complaint".equals(messageType)) {
                // Complaint validation - requires branch
                String branchName = branchComboBox.getValue();
                if (branchName == null) {
                    showAlert("Please select a branch for complaints.");
                    return;
                }

                // Get branch ID (position + 1)
                int branchId = -1;
                for (int i = 0; i < branchNames.length; i++) {
                    if (branchNames[i].equals(branchName)) {
                        branchId = i + 1;
                        break;
                    }
                }

                if (branchId == -1) {
                    showAlert("Invalid branch selection.");
                    return;
                }

                // Format: CREATE_COMPLAINT;phoneNumber;branchId;category;description;optionalOrderId
                String complaintMsg = String.format("CREATE_COMPLAINT;%s;%d;%s;%s;", 
                        phoneNumber, branchId, category, message);
                
                SimpleClient.getClient().sendToServer(complaintMsg);
                System.out.println("Complaint sent to server: " + complaintMsg);
                statusLabel.setText("⏳ Submitting your complaint...");
            } else {
                // General message - branch is optional
                String branchName = branchComboBox.getValue();
                Integer branchId = null;
                if (branchName != null) {
                    for (int i = 0; i < branchNames.length; i++) {
                        if (branchNames[i].equals(branchName)) {
                            branchId = i + 1;
                            break;
                        }
                    }
                }

                // Format: CREATE_GENERAL_MESSAGE;phoneNumber;branchId;category;description
                String generalMsg = String.format("CREATE_GENERAL_MESSAGE;%s;%s;%s;%s", 
                        phoneNumber, 
                        branchId != null ? branchId.toString() : "",
                        category, 
                        message);
                
                SimpleClient.getClient().sendToServer(generalMsg);
                System.out.println("General message sent to server: " + generalMsg);
                statusLabel.setText("⏳ Sending your message...");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Failed to send message: " + e.getMessage());
            statusLabel.setText("❌ Failed to send message.");
        }
    }

    // Handle server responses
    @Subscribe
    public void onWarningEvent(WarningEvent event) {
        String message = event.getWarning().getMessage();
        System.out.println("ContactUsController: Received warning event: " + message);
        
        javafx.application.Platform.runLater(() -> {
            if (message.startsWith("CREATE_COMPLAINT_SUCCESS")) {
                statusLabel.setText("✅ Complaint submitted successfully!");
                messageArea.clear();
                loadPastMessages();
            } else if (message.startsWith("CREATE_COMPLAINT_FAILURE")) {
                String errorMsg = message.substring(message.indexOf(":") + 1).trim();
                statusLabel.setText("❌ " + errorMsg);
                showAlert("Failed to submit complaint: " + errorMsg);
            } else if (message.startsWith("CREATE_GENERAL_MESSAGE_SUCCESS")) {
                statusLabel.setText("✅ Message sent successfully!");
                messageArea.clear();
                loadPastMessages();
            } else if (message.startsWith("CREATE_GENERAL_MESSAGE_FAILURE")) {
                String errorMsg = message.substring(message.indexOf(":") + 1).trim();
                statusLabel.setText("❌ " + errorMsg);
                showAlert("Failed to send message: " + errorMsg);
            }
        });
    }

    private void loadPastMessages() {
        String phoneNumber = SimpleClient.getCurrentUserPhone();
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return; // Not logged in, can't load messages
        }

        try {
            // Request both complaints and general messages from server
            SimpleClient.getClient().sendToServer("GET_USER_COMPLAINTS;" + phoneNumber);
            SimpleClient.getClient().sendToServer("GET_USER_GENERAL_MESSAGES;" + phoneNumber);
            System.out.println("Requesting messages for phone: " + phoneNumber);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to request messages: " + e.getMessage());
        }
    }


    private List<Complaint> cachedComplaints = new java.util.ArrayList<>();
    private List<GeneralMessage> cachedGeneralMessages = new java.util.ArrayList<>();

    // Handle receiving complaints list from server
    @Subscribe
    public void onComplaintsReceived(SimpleClient.ComplaintsReceivedEvent event) {
        List<Complaint> complaints = event.getComplaints();
        javafx.application.Platform.runLater(() -> {
            cachedComplaints = complaints != null ? complaints : new java.util.ArrayList<>();
            refreshMessageDisplay();
        });
    }

    // Handle receiving general messages list from server
    @Subscribe
    public void onGeneralMessagesReceived(SimpleClient.GeneralMessagesReceivedEvent event) {
        javafx.application.Platform.runLater(() -> {
            cachedGeneralMessages = event.getMessages() != null ? event.getMessages() : new java.util.ArrayList<>();
            refreshMessageDisplay();
        });
    }

    private void refreshMessageDisplay() {
        pastMessagesBox.getChildren().clear();
        
        if ((cachedComplaints == null || cachedComplaints.isEmpty()) && 
            (cachedGeneralMessages == null || cachedGeneralMessages.isEmpty())) {
            Label noMessagesLabel = new Label("No messages yet.");
            pastMessagesBox.getChildren().add(noMessagesLabel);
            return;
        }

        // Display general messages first
        if (cachedGeneralMessages != null) {
            for (GeneralMessage msg : cachedGeneralMessages) {
                String labelText = String.format("💬 [General] %s  |  %s", 
                        msg.getCategory(), 
                        msg.getDescription().length() > 50 ? msg.getDescription().substring(0, 50) + "..." : msg.getDescription());
                Label messageLabel = new Label(labelText);
                messageLabel.setWrapText(true);
                pastMessagesBox.getChildren().add(messageLabel);
            }
        }

        // Display complaints
        if (cachedComplaints != null) {
            for (Complaint complaint : cachedComplaints) {
                // Extract category from description (format: [Category] description)
                String description = complaint.getDescription();
                String category = "Other";
                String descText = description;
                
                if (description.startsWith("[") && description.contains("]")) {
                    int endIndex = description.indexOf("]");
                    category = description.substring(1, endIndex);
                    descText = description.substring(endIndex + 1).trim();
                }

                String status = complaint.getStatus().toString();
                
                // Create a VBox for each complaint to show more details
                VBox complaintBox = new VBox(5);
                complaintBox.setStyle("-fx-padding: 10px; -fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 5px;");
                
                // Main complaint info
                String mainText = String.format("📌 [Complaint] %s  |  Status: %s  |  %s", 
                        category, status, descText.length() > 50 ? descText.substring(0, 50) + "..." : descText);
                Label complaintLabel = new Label(mainText);
                complaintLabel.setWrapText(true);
                complaintLabel.setStyle("-fx-font-weight: bold;");
                complaintBox.getChildren().add(complaintLabel);
                
                // Show manager's response if it exists
                if (complaint.getManagerNotes() != null && !complaint.getManagerNotes().trim().isEmpty()) {
                    Label responseLabel = new Label("💬 Manager's Response: " + complaint.getManagerNotes());
                    responseLabel.setWrapText(true);
                    responseLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50; -fx-font-style: italic; -fx-padding: 5px 0 0 15px;");
                    complaintBox.getChildren().add(responseLabel);
                }
                
                pastMessagesBox.getChildren().add(complaintBox);
            }
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    private void handleBack() {
        // Clean up EventBus registration
        cleanup();
        try {
            App.setRoot("personal_area");
        } catch (IOException e) {
            showAlert("Failed to return to personal area.");
        }
    }
    
    // Cleanup method
    public void cleanup() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
} 