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

	private boolean isRequestingOpeningHours = false;

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
	void handlePersonalArea() {
		try {
			App.setRoot("sign_in");
		} catch (IOException e) {
			showError("Failed to load personal area.");
			e.printStackTrace();
		}
	}

	@FXML
	void handleNewUserHelp() {
		String helpMessage = "📱 New User Guide\n\n" +
				"Welcome to Restaurant Kitchen!\n\n" +
				"When you make a reservation or place an order, you can track it in your Personal Area.\n\n" +
				"🔑 Access Your Personal Area:\n" +
				"• Username: Your phone number\n" +
				"• Password: Your name + 123\n\n" +
				"Example:\n" +
				"If your name is 'John' and phone is '0501234567':\n" +
				"• Username: 0501234567\n" +
				"• Password: John123\n\n" +
				"💡 Tip: You can view and manage all your reservations and orders from your Personal Area!";
		
		showInfo("New User Help", helpMessage);
	}

	@FXML
	void handleOpeningHours() {
		// Prevent multiple simultaneous requests
		if (isRequestingOpeningHours) {
			System.out.println("🔵 Opening hours request already in progress, ignoring duplicate click");
			return;
		}
		
		System.out.println("🔵 handleOpeningHours() method called!");
		try {
			isRequestingOpeningHours = true;
			// Request all branches with opening hours from server
			SimpleClient.getClient().sendToServer("GET_ALL_BRANCHES");
			System.out.println("🔵 GET_ALL_BRANCHES request sent to server");
		} catch (IOException e) {
			isRequestingOpeningHours = false;
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
		// Reset the flag when we receive the response
		isRequestingOpeningHours = false;
		
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
		// Only process if we're actually requesting opening hours (prevent processing unrelated branch events)
		if (!isRequestingOpeningHours) {
			System.out.println("🔵 Received branches event but not requesting opening hours, ignoring");
			return;
		}
		
		// Use Platform.runLater to ensure thread safety and prevent duplicate dialogs
		Platform.runLater(() -> {
			// Double-check flag to prevent multiple dialogs (in case event is received multiple times)
			if (isRequestingOpeningHours) {
				displayOpeningHours(event.getBranches());
			}
		});
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
		// Only register if not already registered to prevent duplicate registrations
		if (!EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().register(this);
			System.out.println("🔵 PrimaryController: EventBus registered");
		} else {
			System.out.println("🔵 PrimaryController: Already registered with EventBus, skipping");
		}
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
	 * Show information dialog with custom title and header
	 */
	private void showInfo(String title, String msg) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}

	/**
	 * Show information dialog (for backward compatibility)
	 */
	private void showInfo(String msg) {
		showInfo("Opening Hours", msg);
	}
}
