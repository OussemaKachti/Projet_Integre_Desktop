package com.esprit;

import com.esprit.utils.NavigationManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Classe principale pour lancer l'application d'administration des sondages
 * JavaFX
 */
public class AdminPollsApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Configurer le NavigationManager avec le stage principal
            NavigationManager.setMainStage(primaryStage);

            // Charger la vue d'administration des sondages
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AdminPollsView.fxml"));
            Parent root = loader.load();

            // Créer la scène
            Scene scene = new Scene(root);

            // Configurer la fenêtre principale
            primaryStage.setTitle("Administration des Sondages");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de l'interface: " + e.getMessage());
        }
    }

    /**
     * Point d'entrée principal de l'application
     */
    public static void main(String[] args) {
        launch(args);
    }
}

// FASAKHNI