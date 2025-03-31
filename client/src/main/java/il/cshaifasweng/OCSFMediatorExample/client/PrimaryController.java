package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;

import java.io.IOException;
import java.util.Optional;

public class PrimaryController {

	private static final String MANAGER_PASSWORD = "212101828";

	@FXML
	void handleManagerLogin() {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Manager Login");
		dialog.setHeaderText("Enter Manager Password");
		dialog.setContentText("Password:");

		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			if (result.get().equals(MANAGER_PASSWORD)) {
				try {
					App.setRoot("secondary2"); // Manager-only menu screen
				} catch (IOException e) {
					showError("Failed to load manager view.");
					e.printStackTrace();
				}
			} else {
				showError("Incorrect password!");
			}
		}
	}

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
