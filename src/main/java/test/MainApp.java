// Path: src/main/java/test/MainApp.java
package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            URL loginFxmlUrl = getClass().getResource("/views/login.fxml");
            if (loginFxmlUrl == null) {
                LOGGER.log(Level.SEVERE, "Cannot find /views/login.fxml");
                throw new IllegalStateException("Required FXML file not found: /views/login.fxml");
            }
            
            Parent root = FXMLLoader.load(loginFxmlUrl);
            primaryStage.setTitle("Club Management System");
            primaryStage.setScene(new Scene(root, 500, 400));
            primaryStage.show();
            
            LOGGER.info("Application started successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}