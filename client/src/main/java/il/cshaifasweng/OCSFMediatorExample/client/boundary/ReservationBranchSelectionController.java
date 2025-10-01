package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import javafx.fxml.FXML;

import java.io.IOException;

public class ReservationBranchSelectionController {

    @FXML
    private void handleTelAvivBranch() {
        navigateToTableDiagram(1, "Tel-Aviv");
    }

    @FXML
    private void handleHaifaBranch() {
        navigateToTableDiagram(2, "Haifa");
    }

    @FXML
    private void handleJerusalemBranch() {
        navigateToTableDiagram(3, "Jerusalem");
    }

    @FXML
    private void handleBeerShevaBranch() {
        navigateToTableDiagram(4, "Beer-Sheva");
    }

    @FXML
    private void handleBackToMain() {
        try {
            // Navigate back to managers page instead of primary1
            App.setRoot("secondary2");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToTableDiagram(int branchId, String branchName) {
        // Store the selected branch information in SimpleClient
        SimpleClient.setSelectedBranchId(branchId);
        SimpleClient.setSelectedBranchName(branchName);
        
        System.out.println("Selected branch for table reservation: " + branchName + " (ID: " + branchId + ")");
        
        try {
            // Navigate to the tables map page
            App.setRoot("tables_map");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
