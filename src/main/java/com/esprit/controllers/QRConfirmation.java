package com.esprit.controllers;

import com.esprit.models.Evenement;
import com.esprit.models.Participation_event;
import com.esprit.services.ServiceEvent;
import com.esprit.services.ServiceParticipation;
import com.esprit.utils.QRCodeUtil;
import com.google.zxing.WriterException;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class QRConfirmation implements Initializable {

    @FXML
    private Label eventTitleLabel;
    @FXML
    private Label eventDateLabel;
    @FXML
    private Label eventLocationLabel;
    @FXML
    private Label participationStatusLabel;
    @FXML
    private ImageView qrCodeImageView;
    @FXML
    private Button generateQRButton;
    @FXML
    private Button saveQRButton;
    @FXML
    private Button closeButton;

    private Evenement currentEvent;
    private Long currentUserId;
    private ServiceEvent serviceEvent = new ServiceEvent();
    private ServiceParticipation serviceParticipation = new ServiceParticipation();
    private String qrCodeFilePath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up UI components
        saveQRButton.setDisable(true); // Initially disabled until QR is generated

        // Set up action handlers
        generateQRButton.setOnAction(event -> handleGenerateQR());
        saveQRButton.setOnAction(event -> handleSaveQR());
        closeButton.setOnAction(event -> handleClose());
    }

    /**
     * Sets the event data and user ID for QR code generation
     * @param event The event for participation
     * @param userId The current user's ID
     */
    public void setData(Evenement event, Long userId) {
        this.currentEvent = event;
        this.currentUserId = userId;

        // Update UI with event details
        eventTitleLabel.setText(event.getNom_event());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm");
        eventDateLabel.setText(dateFormat.format(event.getStart_date()));

        eventLocationLabel.setText(event.getLieux());

        // Check participation status
        updateParticipationStatus();
    }

    /**
     * Updates the participation status display
     */
    private void updateParticipationStatus() {
        boolean isRegistered = serviceParticipation.participationExists(currentUserId, currentEvent.getId());

        if (isRegistered) {
            participationStatusLabel.setText("✓ Inscrit à cet événement");
            participationStatusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            generateQRButton.setDisable(false);
        } else {
            participationStatusLabel.setText("⨯ Non inscrit à cet événement");
            participationStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            generateQRButton.setDisable(true);

            // Show alert informing user they need to register first
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Inscription requise");
            alert.setHeaderText("Inscription nécessaire");
            alert.setContentText("Vous devez vous inscrire à l'événement avant de pouvoir générer un QR code de confirmation.");
            alert.showAndWait();
        }
    }

    /**
     * Handles the QR code generation action
     */
    private void handleGenerateQR() {
        try {
            // Generate QR code image
            Image qrCodeImage = QRCodeUtil.createQRCodeImage(currentUserId, Long.valueOf(currentEvent.getId()));
            qrCodeImageView.setImage(qrCodeImage);

            String fileName = QRCodeUtil.generateQRCodeFileName(currentUserId, Long.valueOf(currentEvent.getId()));
            qrCodeFilePath = QRCodeUtil.generateQRCode(currentUserId, Long.valueOf(currentEvent.getId()), fileName);


            // Enable save button
            saveQRButton.setDisable(false);

            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("QR Code Généré");
            alert.setHeaderText("QR Code créé avec succès");
            alert.setContentText("Le QR code de confirmation pour votre participation à l'événement a été généré. " +
                    "Vous pouvez l'enregistrer ou le capturer d'écran pour le présenter lors de l'événement.");
            alert.showAndWait();

        } catch (WriterException | IOException e) {
            // Handle error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur de génération du QR code");
            alert.setContentText("Une erreur s'est produite lors de la génération du QR code: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    /**
     * Handles saving the QR code to a user-selected location
     */
    private void handleSaveQR() {
        if (qrCodeImageView.getImage() == null) {
            return;
        }

        // Create file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le QR code");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Images", "*.png")
        );

        // Set default file name
        String defaultFileName = "QRCode_" + currentEvent.getNom_event().replaceAll("\\s+", "_") + ".png";
        fileChooser.setInitialFileName(defaultFileName);

        // Show save dialog
        File file = fileChooser.showSaveDialog(saveQRButton.getScene().getWindow());

        if (file != null) {
            try {
                // Convert JavaFX image to BufferedImage
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(qrCodeImageView.getImage(), null);

                // Write image to file
                ImageIO.write(bufferedImage, "png", file);

                // Show success message
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Enregistrement réussi");
                alert.setHeaderText("QR Code enregistré");
                alert.setContentText("Le QR code a été enregistré avec succès à l'emplacement spécifié.");
                alert.showAndWait();

            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Erreur d'enregistrement");
                alert.setContentText("Une erreur s'est produite lors de l'enregistrement du QR code: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles closing the QR confirmation window
     */
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}