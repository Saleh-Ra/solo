package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.io.IOException;

public class PrimaryController {

	@FXML
	void handleUserMenu() {
		try {
			App.setRoot("user_menu");
		} catch (IOException e) {
			showError("Failed to load user menu.");
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
	void initialize() {
		try {
			SimpleClient.getClient().sendToServer("add client");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void showError(String msg) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}
}
