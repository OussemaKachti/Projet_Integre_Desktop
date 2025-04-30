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

public class ProduitCardApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(ProduitCardApp.class.getName());

    private static Stage primaryStage;

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            navigateTo("/com/esprit/views/produit/AdminProduitView.fxml");

            primaryStage.setTitle("Test Produit Card");
            primaryStage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            e.printStackTrace();
        }
    }

    /**
     * Loads the specified FXML into the main stage
     * @param fxmlPath path to the FXML file (e.g., "/com/esprit/views/produit/AdminProduitView.fxml")
     */
    public static void navigateTo(String fxmlPath) {
        try {
            URL fxmlUrl = ProduitCardApp.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IllegalArgumentException("FXML file not found: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            adjustStageSize();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Navigation failed", e);
            e.printStackTrace();
        }
    }

    public static void adjustStageSize() {
        if (primaryStage == null) return;

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setWidth(400);  // adapt to card size
        primaryStage.setHeight(300);
        primaryStage.setMinWidth(300);
        primaryStage.setMinHeight(250);
        primaryStage.setX((screenBounds.getWidth() - primaryStage.getWidth()) / 2);
        primaryStage.setY((screenBounds.getHeight() - primaryStage.getHeight()) / 2);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
