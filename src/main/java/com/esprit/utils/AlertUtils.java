package com.esprit.utils;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class AlertUtils {

    private static final Queue<Toast> toastQueue = new LinkedList<>();
    private static boolean isShowingToast = false;

    // Type d'alerte
    public enum AlertType {
        INFORMATION, SUCCESS, ERROR, WARNING
    }

    // Type de toast
    public enum ToastType {
        SUCCESS, ERROR, WARNING, INFO
    }

    private static final Queue<Toast> toastQueue = new LinkedList<>();
    private static boolean isShowingToast = false;

    // Type d'alerte
    public enum AlertType {
        INFORMATION, SUCCESS, ERROR, WARNING
    }

    // Type de toast
    public enum ToastType {
        SUCCESS, ERROR, WARNING, INFO
    }

    /**
     * Affiche une alerte d'information
     * @param title titre de l'alerte
     * @param message message à afficher
     */
    public static void showInformation(String title, String message) {
        showAlert(title, message, AlertType.INFORMATION);
    }

    /**
     * Affiche une alerte de succès
     * @param title titre de l'alerte
     * @param message message à afficher
     */
    public static void showSuccess(String title, String message) {
        showAlert(title, message, AlertType.SUCCESS);
    }

    /**
     * Affiche une alerte d'erreur
     * @param title titre de l'alerte
     * @param message message à afficher
     */
    public static void showError(String title, String message) {
        showAlert(title, message, AlertType.ERROR);
    }

    /**
     * Affiche une alerte d'avertissement
     * @param title titre de l'alerte
     * @param message message à afficher
     */
    public static void showWarning(String title, String message) {
        showAlert(title, message, AlertType.WARNING);
    }

    /**
     * Affiche une alerte de confirmation avec des boutons personnalisés
     * @param title titre de l'alerte
     * @param message message à afficher
     * @return vrai si l'utilisateur confirme, faux sinon
     */
    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        ButtonType okButton = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(okButton, cancelButton);
        
        setupAlertDialog(alert, AlertType.SUCCESS);
        
        Button confirmButton = (Button) alert.getDialogPane().lookupButton(okButton);
        confirmButton.getStyleClass().add("ok-button");
        
        Button cancelBtn = (Button) alert.getDialogPane().lookupButton(cancelButton);
        cancelBtn.getStyleClass().add("cancel-button");
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == okButton;
    }

    /**
     * Affiche une alerte avec animation et style personnalisé
     * @param title titre de l'alerte
     * @param message message à afficher
     * @param type type d'alerte
     */
    private static void showAlert(String title, String message, AlertType type) {
        Platform.runLater(() -> {
            Alert.AlertType alertType;
            String styleClass;
            
            switch (type) {
                case INFORMATION:
                    alertType = Alert.AlertType.INFORMATION;
                    styleClass = "information";
                    break;
                case SUCCESS:
                    alertType = Alert.AlertType.INFORMATION;
                    styleClass = "confirmation";
                    break;
                case ERROR:
                    alertType = Alert.AlertType.ERROR;
                    styleClass = "error";
                    break;
                case WARNING:
                    alertType = Alert.AlertType.WARNING;
                    styleClass = "warning";
                    break;
                default:
                    alertType = Alert.AlertType.INFORMATION;
                    styleClass = "information";
                    break;
            }
            
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            setupAlertDialog(alert, type);
            alert.getDialogPane().getStyleClass().add(styleClass);
            
            Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            okButton.getStyleClass().add("ok-button");
            
            alert.showAndWait();
        });
    }

    /**
     * Configure le style et l'animation de l'alerte
     * @param alert l'alerte à configurer
     * @param type type d'alerte
     */
    private static void setupAlertDialog(Alert alert, AlertType type) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(AlertUtils.class.getResource("/com/esprit/styles/alert-style.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("alert-animation");
        
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.setAlwaysOnTop(true);
        stage.initStyle(StageStyle.UNDECORATED);
    }

    /**
     * Affiche un toast avec animation
     * @param message message à afficher
     * @param type type de toast
     * @param durationInSeconds durée d'affichage en secondes
     */
    public static void showToast(String message, ToastType type, double durationInSeconds) {
        Toast toast = new Toast(message, type, durationInSeconds);
        toastQueue.add(toast);
        
        if (!isShowingToast) {
            showNextToast();
        }
    }

    /**
     * Affiche le prochain toast dans la file d'attente
     */
    private static void showNextToast() {
        if (toastQueue.isEmpty()) {
            isShowingToast = false;
            return;
        }
        
        isShowingToast = true;
        Toast toast = toastQueue.poll();
        
        Platform.runLater(() -> {
            Stage toastStage = new Stage();
            toastStage.initStyle(StageStyle.TRANSPARENT);
            toastStage.setAlwaysOnTop(true);
            toastStage.initModality(Modality.NONE);
            
            Label toastLabel = new Label(toast.getMessage());
            toastLabel.getStyleClass().add("toast-label");
            
            // Ajoute la classe de style en fonction du type
            switch (toast.getType()) {
                case SUCCESS:
                    toastLabel.getStyleClass().add("toast-success");
                    break;
                case ERROR:
                    toastLabel.getStyleClass().add("toast-error");
                    break;
                case WARNING:
                    toastLabel.getStyleClass().add("toast-warning");
                    break;
                case INFO:
                    toastLabel.getStyleClass().add("toast-info");
                    break;
            }
            
            StackPane root = new StackPane(toastLabel);
            root.setAlignment(Pos.BOTTOM_CENTER);
            root.setStyle("-fx-background-color: transparent;");
            root.setPrefHeight(100);
            root.setPrefWidth(350);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(AlertUtils.class.getResource("/com/esprit/styles/alert-style.css").toExternalForm());
            scene.setFill(null);
            
            toastStage.setScene(scene);
            
            // Centrer la fenêtre
            toastStage.setX((javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() - root.getPrefWidth()) / 2);
            toastStage.setY(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() - 150);
            
            // Animation d'entrée
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.3), toastLabel);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            
            // Animation de sortie
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), toastLabel);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                toastStage.close();
                // Afficher le prochain toast s'il y en a
                showNextToast();
            });
            
            // Attendre puis disparaître
            PauseTransition pause = new PauseTransition(Duration.seconds(toast.getDurationInSeconds()));
            pause.setOnFinished(e -> fadeOut.play());
            
            toastStage.show();
            fadeIn.play();
            pause.play();
        });
    }

    /**
     * Classe interne pour représenter un toast
     */
    private static class Toast {
        private final String message;
        private final ToastType type;
        private final double durationInSeconds;

        public Toast(String message, ToastType type, double durationInSeconds) {
            this.message = message;
            this.type = type;
            this.durationInSeconds = durationInSeconds;
        }

        public String getMessage() {
            return message;
        }

        public ToastType getType() {
            return type;
        }

        public double getDurationInSeconds() {
            return durationInSeconds;
        }
    }
} 