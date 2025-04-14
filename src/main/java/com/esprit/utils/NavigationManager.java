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
            // Preserve maximized state
            boolean wasMaximized = mainStage.isMaximized();
            mainStage.setScene(scene);
            if (wasMaximized) {
                mainStage.setMaximized(true);
            }
        } else {
            throw new IllegalStateException("Le stage principal n'a pas été configuré. Appelez setMainStage() d'abord.");
        }
    }
    
    /**
     * Ensures a stage is displayed in maximized mode
     * @param stage the stage to maximize
     */
    public static void ensureFullScreen(Stage stage) {
        if (stage != null) {
            stage.setMaximized(true);
        }
    }
    
    /**
     * Navigue vers une nouvelle scène et force le plein écran
     * 
     * @param scene La scène vers laquelle naviguer
     */
    public static void navigateToFullScreen(Scene scene) {
        if (mainStage != null) {
            mainStage.setScene(scene);
            mainStage.setMaximized(true);
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
    
    /**
     * Obtient le stage principal de l'application
     * 
     * @return Le stage principal
     */
    public static Stage getMainStage() {
        return mainStage;
    }
} 