// Path: src/main/java/test/MainApp.java
package com.esprit;

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

public class ProduitApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(ProduitApp.class.getName());

    private static Stage primaryStage;

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            navigateTo("/com/esprit/views/produit/ProduitView.fxml");

            primaryStage.setTitle("UNICLUBS - Club Management System");
            primaryStage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            e.printStackTrace();
        }
    }

    /**
     * Central navigation method to change scenes
     * @param fxmlPath path to the FXML file (e.g., "/views/produit.fxml")
     */
    public static void navigateTo(String fxmlPath) {
        try {
            URL fxmlUrl = ProduitApp.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IllegalArgumentException("FXML file not found: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            adjustStageSize(false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Navigation failed", e);
            e.printStackTrace();
        }
    }

    public static void adjustStageSize(boolean isLoginScreen) {
        if (primaryStage == null) return;

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        if (isLoginScreen) {
            primaryStage.setMaximized(false);
            primaryStage.setWidth(600);
            primaryStage.setHeight(550);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(550);
            centerStageOnScreen(primaryStage);
        } else {
            primaryStage.setMaximized(true);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
        }
    }

    public static void centerStageOnScreen(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
