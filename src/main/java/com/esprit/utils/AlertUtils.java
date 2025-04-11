package com.esprit.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.util.Optional;
import java.net.URL;
import javafx.scene.control.DialogPane;

/**
 * Classe utilitaire pour afficher des messages d'alerte et des notifications
 */
public class AlertUtils {

    /**
     * Types de notifications toast
     */
    public enum ToastType {
        SUCCESS, ERROR, INFO, WARNING
    }
    
    /**
     * Charge le fichier CSS pour les alertes et notifications
     */
    private static void applyStyleToAlert(Alert alert) {
        URL cssUrl = AlertUtils.class.getResource("/com/esprit/styles/alert-style.css");
        if (cssUrl != null) {
            alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            alert.getDialogPane().getStyleClass().add("alert-dialog");
        }
    }

    /**
     * Affiche une alerte d'information
     *
     * @param title   Titre de l'alerte
     * @param message Message de l'alerte
     */
    public static void showInformation(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Styliser l'alerte
        styleAlert(alert);
        
        alert.showAndWait();
    }

    /**
     * Affiche une alerte d'information (version alternative)
     *
     * @param title   Titre de l'alerte
     * @param message Message de l'alerte
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Information");
        alert.setContentText(message);
        
        // Styliser l'alerte
        styleAlert(alert);
        
        alert.showAndWait();
    }

    /**
     * Affiche une alerte de succès
     *
     * @param title   Titre de l'alerte
     * @param message Message de l'alerte
     */
    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Success");
        alert.setContentText(message);
        
        // Styliser l'alerte
        styleAlert(alert);
        
        alert.showAndWait();
    }

    /**
     * Affiche une alerte d'erreur
     *
     * @param title   Titre de l'alerte
     * @param message Message de l'alerte
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Error");
        alert.setContentText(message);
        
        // Styliser l'alerte
        styleAlert(alert);
        
        alert.showAndWait();
    }

    /**
     * Affiche une alerte d'avertissement
     *
     * @param title   Titre de l'alerte
     * @param message Message de l'alerte
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText("Warning");
        alert.setContentText(message);
        
        // Styliser l'alerte
        styleAlert(alert);
        
        alert.showAndWait();
    }

    /**
     * Affiche une alerte de confirmation avec boutons Oui/Non
     *
     * @param title   Titre de l'alerte
     * @param message Message de l'alerte
     * @return true si l'utilisateur a cliqué sur Oui, false sinon
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText("Confirmation");
        alert.setContentText(message);
        
        // Styliser l'alerte
        styleAlert(alert);
        
        // Personnaliser les boutons
        ButtonType buttonTypeYes = new ButtonType("Yes");
        ButtonType buttonTypeNo = new ButtonType("No");
        
        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == buttonTypeYes;
    }

    /**
     * Affiche une notification toast temporaire
     *
     * @param message   Message à afficher
     * @param duration  Durée d'affichage en secondes
     * @param toastType Type de notification (SUCCESS, ERROR, INFO, WARNING)
     * @param scene     Scene dans laquelle afficher la notification
     */
    public static void showToast(String message, double duration, ToastType toastType, Scene scene) {
        Label toastLabel = new Label(message);
        toastLabel.getStyleClass().add("toast-label");
        
        // Appliquer la couleur selon le type
        String cssColor = switch (toastType) {
            case SUCCESS -> "toast-success";
            case ERROR -> "toast-error";
            case WARNING -> "toast-warning";
            default -> "toast-info";
        };
        
        toastLabel.getStyleClass().add(cssColor);
        
        StackPane toastPane = new StackPane(toastLabel);
        toastPane.setAlignment(Pos.BOTTOM_CENTER);
        toastPane.setMouseTransparent(true);
        
        StackPane root = (StackPane) scene.getRoot();
        toastPane.setTranslateY(50);
        toastPane.setOpacity(0);
        
        root.getChildren().add(toastPane);
        
        Timeline fadeInTimeline = new Timeline();
        KeyFrame fadeInKey1 = new KeyFrame(Duration.millis(100), new KeyValue(toastPane.opacityProperty(), 0));
        KeyFrame fadeInKey2 = new KeyFrame(Duration.millis(500), new KeyValue(toastPane.opacityProperty(), 1));
        fadeInTimeline.getKeyFrames().addAll(fadeInKey1, fadeInKey2);
        fadeInTimeline.setOnFinished(e -> {
            Timeline fadeOutTimeline = new Timeline();
            KeyFrame fadeOutKey1 = new KeyFrame(Duration.seconds(duration), new KeyValue(toastPane.opacityProperty(), 1));
            KeyFrame fadeOutKey2 = new KeyFrame(Duration.seconds(duration + 0.5), new KeyValue(toastPane.opacityProperty(), 0));
            fadeOutTimeline.getKeyFrames().addAll(fadeOutKey1, fadeOutKey2);
            fadeOutTimeline.setOnFinished(event -> root.getChildren().remove(toastPane));
            fadeOutTimeline.play();
        });
        fadeInTimeline.play();
    }
    
    /**
     * Affiche une notification de succès
     * 
     * @param message Message
     * @param scene Scene
     */
    public static void showSuccess(String message, Scene scene) {
        showToast(message, 3, ToastType.SUCCESS, scene);
    }
    
    /**
     * Affiche une notification d'erreur
     * 
     * @param message Message
     * @param scene Scene
     */
    public static void showToastError(String message, Scene scene) {
        showToast(message, 3, ToastType.ERROR, scene);
    }
    
    /**
     * Applique un style personnalisé à l'alerte
     *
     * @param alert L'alerte à styliser
     */
    private static void styleAlert(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
            AlertUtils.class.getResource("/com/esprit/styles/poll-management-style.css").toExternalForm()
        );
        dialogPane.getStyleClass().add("custom-alert");
        
        // Supprime le header icon si présent
        if (dialogPane.getHeader() != null) {
            dialogPane.setGraphic(null);
        }
        
        // Style minimal sans barre de titre
        alert.initStyle(StageStyle.UNDECORATED);
    }

    /**
     * Configure l'icône d'une alerte (si nécessaire)
     * 
     * @param alert L'alerte à configurer
     * @param iconPath Le chemin de l'icône à utiliser
     */
    public static void setAlertIcon(Alert alert, String iconPath) {
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        // Utiliser l'icône de l'application si nécessaire
        // stage.getIcons().add(new Image(iconPath));
    }
} 