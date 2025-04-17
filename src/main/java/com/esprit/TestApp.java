package com.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestApp extends Application {
    
    private static Stage primaryStage;
    
    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            
            // Load the AdminProduitView.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/produit/AdminProduitView.fxml"));
            Parent root = loader.load();
            
            // Create and set the scene
            Scene scene = new Scene(root);
            
            // Configure the stage
            stage.setTitle("Admin Interface - Product Management");
            stage.setScene(scene);
            
            // Make the window maximized by default
            stage.setMaximized(true);
            
            // Set minimum window size
            stage.setMinWidth(1200);
            stage.setMinHeight(800);
            
            // Show the stage
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
} 