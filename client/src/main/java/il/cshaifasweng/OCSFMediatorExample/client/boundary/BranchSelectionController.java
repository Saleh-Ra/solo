package il.cshaifasweng.OCSFMediatorExample.client.boundary;

import il.cshaifasweng.OCSFMediatorExample.client.App;
import il.cshaifasweng.OCSFMediatorExample.client.SimpleClient;
import javafx.fxml.FXML;

import java.io.IOException;

public class BranchSelectionController {

    @FXML
    private void handleTelAvivBranch() {
        navigateToBranchMenu(1, "Tel-Aviv");
    }

    @FXML
    private void handleHaifaBranch() {
        navigateToBranchMenu(2, "Haifa");
    }

    @FXML
    private void handleJerusalemBranch() {
        navigateToBranchMenu(3, "Jerusalem");
    }

    @FXML
    private void handleBeerShevaBranch() {
        navigateToBranchMenu(4, "Beer-Sheva");
    }

    @FXML
    private void handleBackToMain() {
        try {
            App.setRoot("primary1");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToBranchMenu(int branchId, String branchName) {
        // Store the selected branch information in SimpleClient
        SimpleClient.setSelectedBranchId(branchId);
        SimpleClient.setSelectedBranchName(branchName);
        
        System.out.println("Selected branch: " + branchName + " (ID: " + branchId + ")");
        
        try {
            // Navigate to the user menu page
            App.setRoot("user_menu");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
