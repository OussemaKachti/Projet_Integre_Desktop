package com.esprit;

import java.io.IOException;
import java.time.LocalDateTime;

import com.esprit.models.User;
import com.esprit.models.enums.RoleEnum;
import com.esprit.utils.SessionManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp2 extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Create a test admin user and set it in the session for testing
            createAndSetTestAdminUser();
            
            // Load the admin profile view instead of AjouterEvent
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/admin_profile.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            // Apply the correct stylesheet for the admin profile
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());
            
            // Set the stage properties
            primaryStage.setTitle("Admin Profile - UNICLUBS");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true); // Open maximized for better testing
            primaryStage.show();
        } catch (IOException e) {
            System.out.println("Error loading admin profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a test admin user and sets it in the session for testing purposes
     */
    private void createAndSetTestAdminUser() {
        User testAdmin = new User();
        testAdmin.setId(1);
        testAdmin.setFirstName("admin");
        testAdmin.setLastName("admin");
        testAdmin.setEmail("admin@test.com");
        testAdmin.setPhone("99999999");
        testAdmin.setRole(RoleEnum.ADMINISTRATEUR);
        testAdmin.setCreatedAt(LocalDateTime.now());
        testAdmin.setLastLoginAt(LocalDateTime.now());
        
        // Set the test user in the session
        SessionManager.getInstance().setCurrentUser(testAdmin);
    }
}
