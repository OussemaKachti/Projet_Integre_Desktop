// Path: src/main/java/com/esprit/MainApp.java
package com.esprit;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.esprit.services.UserService;

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
    private static final double LOGIN_WIDTH = 700;
    private static final double LOGIN_HEIGHT = 700;
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
        // Reset database connections by ensuring MySQL has enough connections
        resetDatabaseConnections();
        
        // Store reference to primary stage
        primaryStage = stage;
        
        URL loginFxmlUrl = getClass().getResource("/com/esprit/views/login.fxml");
        if (loginFxmlUrl == null) {
            LOGGER.log(Level.SEVERE, "Cannot find /com/esprit/views/login.fxml");
            throw new IllegalStateException("Required FXML file not found: /com/esprit/views/login.fxml");
        }
        
        Parent root = FXMLLoader.load(loginFxmlUrl);
        
        // Use our utility method with explicit larger dimensions for login
        setupStage(primaryStage, root, "UNICLUBS - Club Management System", true, 700, 700);
        
        // Explicitly center on screen before showing
        centerStageOnScreen(primaryStage);
        
        // Show the stage
        primaryStage.show();
        
        // Ensure stage is centered after showing
        ensureCentered(primaryStage);
        
        // Add shutdown hook to close database connections
        primaryStage.setOnCloseRequest(event -> {
            cleanup();
        });
        
        LOGGER.info("Application started successfully");
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to start application", e);
        e.printStackTrace();
    }
}

    @Override
    public void stop() {
        // Ensure all resources are closed when the application exits
        cleanup();
        LOGGER.info("Application stopped, all resources cleaned up");
    }
    
    /**
     * Cleans up resources when the application is closing
     */
    private void cleanup() {
        try {
            // Close the EntityManagerFactory to free up database connections
            UserService.closeEntityManagerFactory();
            LOGGER.info("Database connections closed");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing resources", e);
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
     * Ensures a stage is properly centered on screen
     * This can be called after the stage is displayed to fix positioning issues
     * @param stage The stage to center
     */
    public static void ensureCentered(Stage stage) {
        // Force the stage to layout its content first
        stage.requestFocus();
        // Apply center position
        centerStageOnScreen(stage);
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
            primaryStage.setResizable(true);
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
    //////////////// tajrba 
    ///// Add at the end of the MainApp class but before the closing brace
public static void setupStage(Stage stage, Parent root, String title, boolean isLoginScreen) {
    // Create scene without explicit dimensions
    Scene scene = new Scene(root);
    stage.setScene(scene);
    stage.setTitle(title);
    
    // Apply the appropriate sizing based on screen type
    if (isLoginScreen) {
        // Login/register/verification screens
        configureLightweightScreen(stage);
        
        // Add a listener to ensure window is centered when shown
        stage.setOnShown(e -> ensureCentered(stage));
    } else {
        // Main application screens (dashboard, profile, etc.)
        configureMainApplicationScreen(stage);
    }
}

// Overloaded setupStage that allows for custom width and height
public static void setupStage(Stage stage, Parent root, String title, boolean isLoginScreen, double width, double height) {
    // Create scene with explicit dimensions
    Scene scene = new Scene(root);
    stage.setScene(scene);
    stage.setTitle(title);
    
    if (isLoginScreen) {
        // Login screens with custom dimensions
        stage.setMaximized(false);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setMinWidth(width);
        stage.setMinHeight(height);
        stage.setResizable(true);
        
        // Position the window in the center of the screen
        centerStageOnScreen(stage);
        
        // Add a listener to ensure window is centered when shown
        stage.setOnShown(e -> ensureCentered(stage));
    } else {
        // Main application screens
        configureMainApplicationScreen(stage);
    }
}

private static void configureLightweightScreen(Stage stage) {
    // Fixed size for login-type screens, but make it large enough for content
    stage.setMaximized(false);
    stage.setWidth(LOGIN_WIDTH);
    stage.setHeight(LOGIN_HEIGHT);
    stage.setMinWidth(LOGIN_WIDTH);
    stage.setMinHeight(LOGIN_HEIGHT);
    
    // Add resizable option to let users resize if content is still not fitting
    stage.setResizable(true);
    
    // Ensure stage is centered both initially and after being shown
    centerStageOnScreen(stage);
    stage.setOnShown(e -> ensureCentered(stage));
}

private static void configureMainApplicationScreen(Stage stage) {
    // Main application screens should be maximized
    stage.setMaximized(true);
    stage.setMinWidth(900);
    stage.setMinHeight(600);
}

private void resetDatabaseConnections() {
    try {
        LOGGER.info("Initializing database connection pool...");
        
        // Connect to MySQL directly to reset connections if needed
        java.sql.Connection connection = null;
        try {
            // Load JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Create a direct connection to MySQL to run administrative commands
            connection = java.sql.DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/", "root", "");
            
            // Execute a command to show process list and look for sleep connections
            java.sql.Statement stmt = connection.createStatement();
            
            // Just check connection works - don't actually kill connections
            stmt.execute("SELECT 1");
            LOGGER.info("Database connection successful");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not reset database connections: {0}", e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error closing connection: {0}", e.getMessage());
                }
            }
        }
    } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Database connection reset failed: {0}", e.getMessage());
    }
}
}