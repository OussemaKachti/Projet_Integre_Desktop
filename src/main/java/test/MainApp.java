// Path: src/main/java/test/MainApp.java
package test;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());
    
    // Define standard window sizes
    private static final double LOGIN_WIDTH = 600;
    private static final double LOGIN_HEIGHT = 550;
    private static final double MAIN_WIDTH = 1280;
    private static final double MAIN_HEIGHT = 800;
    
    // Store the primary stage as a static reference for access from controllers
    private static Stage primaryStage;
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage stage) {
        try {
            // Store reference to primary stage
            primaryStage = stage;
            
            URL loginFxmlUrl = getClass().getResource("/views/login.fxml");
            if (loginFxmlUrl == null) {
                LOGGER.log(Level.SEVERE, "Cannot find /views/login.fxml");
                throw new IllegalStateException("Required FXML file not found: /views/login.fxml");
            }
            
            Parent root = FXMLLoader.load(loginFxmlUrl);
            
            // Configure the primary stage
            primaryStage.setTitle("UNICLUBS - Club Management System");
            primaryStage.setMinWidth(LOGIN_WIDTH);
            primaryStage.setMinHeight(LOGIN_HEIGHT);
            
            // Create scene with appropriate size
            Scene scene = new Scene(root, LOGIN_WIDTH, LOGIN_HEIGHT);
            primaryStage.setScene(scene);
            
            // Center the window on screen
            centerStageOnScreen(primaryStage);
            
            // Show the stage
            primaryStage.show();
            
            LOGGER.info("Application started successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Centers a stage on the primary screen
     * @param stage The stage to center
     */
    public static void centerStageOnScreen(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
    }
    
    /**
     * Changes scene size for different parts of the application
     * @param isLoginScreen Whether this is the login/register screen
     */
    public static void adjustStageSize(boolean isLoginScreen) {
        if (primaryStage == null) return;
        
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        
        if (isLoginScreen) {
            // Login screens have centered, fixed size
            primaryStage.setMaximized(false);
            primaryStage.setWidth(LOGIN_WIDTH);
            primaryStage.setHeight(LOGIN_HEIGHT);
            primaryStage.setMinWidth(LOGIN_WIDTH);
            primaryStage.setMinHeight(LOGIN_HEIGHT);
            centerStageOnScreen(primaryStage);
        } else {
            // Main screens should be maximized on the screen
            primaryStage.setMaximized(true);
            
            // Set minimum sizes so it can be unmaximized if needed
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
        }
    }
    
    /**
     * Makes any stage fill the screen
     * @param stage The stage to maximize
     */
    public static void maximizeStage(Stage stage) {
        if (stage == null) return;
        stage.setMaximized(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}