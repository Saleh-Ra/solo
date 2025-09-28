package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import java.io.IOException;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;

public class PrimaryController implements Initializable {

	@FXML
	void handleUserMenu() {
		try {
			App.setRoot("branch_selection");
		} catch (IOException e) {
			showError("Failed to load branch selection.");
			e.printStackTrace();
		}
	}

	@FXML
	void handleMakeReservation() {
		try {
			App.setRoot("reservation");
		} catch (IOException e) {
			showError("Failed to load reservation screen.");
			e.printStackTrace();
		}
	}

	@FXML
	void handleTableDiagram() {
		try {
			App.setRoot("reservation_branch_selection");
		} catch (IOException e) {
			showError("Failed to load reservation branch selection screen.");
			e.printStackTrace();
		}
	}

	@FXML
	void handlePersonalArea() {
		try {
			App.setRoot("sign_in");
		} catch (IOException e) {
			showError("Failed to load personal area.");
			e.printStackTrace();
		}
	}

	@FXML
	void handleCancelReservation() {
		try {
			// For now, navigate to personal area where users can see and cancel their reservations
			// This is a simple approach - users can see their reservations and cancel them there
			App.setRoot("sign_in");
		} catch (IOException e) {
			showError("Failed to load personal area for reservation management.");
			e.printStackTrace();
		}
	}

	@FXML
	void handleOpeningHours() {
		System.out.println("🔵 handleOpeningHours() method called!");
		System.out.println("🔵 Button click detected!");
		try {
			System.out.println("🔵 Opening Hours button clicked!");
			// Request all branches with opening hours from server
			SimpleClient.getClient().sendToServer("GET_ALL_BRANCHES");
			System.out.println("🔵 GET_ALL_BRANCHES request sent to server");
		} catch (IOException e) {
			System.out.println("🔵 Error in handleOpeningHours: " + e.getMessage());
			showError("Failed to request opening hours: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@FXML
	void handleBackToUserType() {
		try {
			// Navigate back to user type selection page
			App.setRoot("user_type_selection");
		} catch (IOException e) {
			showError("Failed to return to user type selection.");
			e.printStackTrace();
		}
	}

	/**
	 * Display opening hours for all branches
	 */
	public void displayOpeningHours(List<Branch> branches) {
		if (branches == null || branches.isEmpty()) {
			Platform.runLater(() -> showInfo("No branch information available."));
			return;
		}

		StringBuilder hoursInfo = new StringBuilder();
		hoursInfo.append("🕒 Restaurant Opening Hours:\n\n");
		
		for (Branch branch : branches) {
			hoursInfo.append("📍 ").append(branch.getLocation()).append("\n");
			hoursInfo.append("   ").append(branch.getOpeningHours()).append("\n\n");
		}
		
		final String finalHoursInfo = hoursInfo.toString();
		Platform.runLater(() -> showInfo(finalHoursInfo));
	}

	/**
	 * Handle incoming messages from server
	 */
	@Subscribe
	public void onBranchesReceived(SimpleClient.BranchesReceivedEvent event) {
		displayOpeningHours(event.getBranches());
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		System.out.println("🔵 PrimaryController: initialize() called");
		System.out.println("🔵 PrimaryController: FXML loaded successfully");
		System.out.println("🔵 PrimaryController: Controller instance: " + this);
		try {
			SimpleClient.getClient().sendToServer("add client");
		} catch (IOException e) {
			e.printStackTrace();
		}
		EventBus.getDefault().register(this);
		System.out.println("🔵 PrimaryController: EventBus registered");
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		EventBus.getDefault().unregister(this);
	}

	private void showError(String msg) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Info");
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}

	/**
	 * Show information dialog
	 */
	private void showInfo(String msg) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Opening Hours");
		alert.setHeaderText("Restaurant Branch Information");
		alert.setContentText(msg);
		alert.showAndWait();
	}
}
