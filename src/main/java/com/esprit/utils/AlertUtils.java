package com.esprit.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Classe utilitaire pour afficher des alertes et des boîtes de dialogue de confirmation.
 */
public class AlertUtils {

    /**
     * Affiche une boîte de dialogue d'information.
     *
     * @param title Le titre de la boîte de dialogue
     * @param message Le message à afficher
     */
    public static void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Affiche une boîte de dialogue d'erreur.
     *
     * @param title Le titre de la boîte de dialogue
     * @param message Le message d'erreur à afficher
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Affiche une boîte de dialogue d'avertissement.
     *
     * @param title Le titre de la boîte de dialogue
     * @param message Le message d'avertissement à afficher
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Affiche une boîte de dialogue de confirmation.
     *
     * @param title Le titre de la boîte de dialogue
     * @param message Le message de confirmation à afficher
     * @return true si l'utilisateur a confirmé, false sinon
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        styleAlert(alert);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Applique un style personnalisé à la boîte de dialogue.
     *
     * @param alert La boîte de dialogue à styliser
     */
    private static void styleAlert(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(AlertUtils.class.getResource("/com/esprit/styles/alert-style.css").toExternalForm());
        dialogPane.getStyleClass().add("alert-dialog");
        
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.setAlwaysOnTop(true);
    }
} 