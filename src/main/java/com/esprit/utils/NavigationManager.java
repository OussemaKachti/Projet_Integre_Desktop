package com.esprit.utils;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Gestionnaire de navigation pour l'application
 * Cette classe permet de naviguer entre les différentes scènes de l'application
 */
public class NavigationManager {
    
    private static Stage mainStage;
    
    /**
     * Configure le stage principal de l'application
     * 
     * @param stage Le stage principal
     */
    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }
    
    /**
     * Navigue vers une nouvelle scène
     * 
     * @param scene La scène vers laquelle naviguer
     */
    public static void navigateTo(Scene scene) {
        if (mainStage != null) {
            mainStage.setScene(scene);
        } else {
            throw new IllegalStateException("Le stage principal n'a pas été configuré. Appelez setMainStage() d'abord.");
        }
    }
    
    /**
     * Obtient la scène actuelle
     * 
     * @return La scène actuelle
     */
    public static Scene getCurrentScene() {
        if (mainStage != null) {
            return mainStage.getScene();
        }
        return null;
    }
} 