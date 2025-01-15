package il.cshaifasweng.OCSFMediatorExample.client;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class PrimaryController {

	@FXML
	void sendWarning(ActionEvent event) {
		try {
			SimpleClient.getClient().sendToServer("#warning");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	void handleViewMenu() {
		System.out.println("View Menu button clicked"); // Debug message
		try {
			App.setRoot("secondary2");
			System.out.println("after try");
		} catch (IOException e) {
			System.out.println("after catch");
			e.printStackTrace();
		}
	}
// nothing

	@FXML
	void initialize() {
		try {
			SimpleClient.getClient().sendToServer("add client");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
