package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Report;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

public class ReportsViewController {
    @FXML
    private Label branchLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<Report.ReportType> reportTypeComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextArea reportsTextArea;

    @FXML
    private void initialize() {
        // Set branch name
        String branchName = SimpleClient.getClient().getBranchName();
        branchLabel.setText(" - " + (branchName != null ? branchName : "Unknown Branch"));

        // Initialize report types combo box
        reportTypeComboBox.setItems(FXCollections.observableArrayList(Report.ReportType.values()));
        
        // Set default dates
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
        endDatePicker.setValue(LocalDate.now());

        // Initialize reports text area
        reportsTextArea.setEditable(false);
        reportsTextArea.setWrapText(true);
        reportsTextArea.setPrefRowCount(15);

        // Register with EventBus to receive report updates
        EventBus.getDefault().register(this);
        
        // Load existing reports
        loadReports();
    }

    @FXML
    private void handleGenerateReport() {
        Report.ReportType selectedType = reportTypeComboBox.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (selectedType == null) {
            showError("Please select a report type");
            return;
        }

        if (startDate == null || endDate == null) {
            showError("Please select both start and end dates");
            return;
        }

        if (endDate.isBefore(startDate)) {
            showError("End date cannot be before start date");
            return;
        }

        try {
            // Convert LocalDate to LocalDateTime (start of day and end of day)
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

            // Send generate report request to server
            SimpleClient.getClient().sendToServer(String.format("GENERATE_REPORT;%s;%s;%s",
                selectedType, startDateTime, endDateTime));
            
            statusLabel.setText("Generating report...");
        } catch (IOException e) {
            showError("Error generating report: " + e.getMessage());
        }
    }



    @FXML
    private void handleExport() {
        String currentReport = reportsTextArea.getText();
        if (currentReport.isEmpty()) {
            showError("No report to export");
            return;
        }

        try {
            SimpleClient.getClient().sendToServer("EXPORT_REPORT;current");
            statusLabel.setText("Exporting current report...");
        } catch (IOException e) {
            showError("Error exporting report: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLoadReports() {
        try {
            SimpleClient.getClient().sendToServer("GET_REPORTS");
            statusLabel.setText("Loading reports...");
        } catch (IOException e) {
            showError("Error loading reports: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefresh() {
        handleLoadReports();
    }

    private void loadReports() {
        // Auto-load reports when page opens
        handleLoadReports();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Event handling methods
    @Subscribe
    public void onReportReceived(SimpleClient.ReportReceivedEvent event) {
        Platform.runLater(() -> {
            reportsTextArea.setText(event.getReportData());
            statusLabel.setText("Report loaded successfully");
        });
    }
    
    @Subscribe
    public void onReportError(SimpleClient.ReportErrorEvent event) {
        Platform.runLater(() -> {
            showError("Report Error: " + event.getErrorMessage());
            statusLabel.setText("Error loading report");
        });
    }
    
    @Subscribe
    public void onReportExport(SimpleClient.ReportExportEvent event) {
        Platform.runLater(() -> {
            statusLabel.setText(event.getExportMessage());
        });
    }
    
    @FXML
    private void handleBack() {
        // Unregister from EventBus
        EventBus.getDefault().unregister(this);
        
        try {
            App.setRoot("secondary2");
        } catch (IOException e) {
            showError("Error returning to dashboard: " + e.getMessage());
        }
    }
} 