package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private SimpleClient client;

    @Override
    public void start(Stage stage) throws IOException {
        EventBus.getDefault().register(this);
        client = SimpleClient.getClient();
        client.openConnection();

        // Load the initial scene
        scene = new Scene(loadFXML("primary1"), 640, 480);
        stage.setScene(scene);
        stage.setTitle("Restaurant Management System");
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        String resourcePath = "/il/cshaifasweng/OCSFMediatorExample/client/" + fxml + ".fxml";

        System.out.println("Attempting to load FXML from: " + App.class.getResource(resourcePath)); // Debug message

        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(resourcePath));

        if (fxmlLoader.getLocation() == null) {
            throw new IOException("FXML file not found at: " + resourcePath);
        }

        return fxmlLoader.load();
    }


    @Override
    public void stop() throws Exception {
        // Cleanup on application stop
        EventBus.getDefault().unregister(this);
        client.sendToServer("remove client");
        client.closeConnection();
        super.stop();
    }

    @Subscribe
    public void onWarningEvent(WarningEvent event) {
        // Handle warning events from the server
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.WARNING,
                    String.format("Message: %s\nTimestamp: %s\n",
                            event.getWarning().getMessage(),
                            event.getWarning().getTime().toString())
            );
            alert.show();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}