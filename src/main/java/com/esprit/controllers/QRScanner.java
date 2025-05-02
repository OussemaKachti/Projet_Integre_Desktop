package com.esprit.controllers;

import com.esprit.models.Evenement;
import com.esprit.models.Participation_event;
import com.esprit.services.ServiceEvent;
import com.esprit.services.ServiceParticipation;
import com.esprit.services.UserService;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
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
import java.util.*;

public class QRScanner implements Initializable {

    @FXML
    private Label eventTitleLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextArea resultTextArea;
    @FXML
    private ImageView scannedImageView;
    @FXML
    private Button scanButton;
    @FXML
    private Button closeButton;
    @FXML
    private ProgressBar scanProgressBar;

    private Evenement currentEvent;
    private ServiceEvent serviceEvent = new ServiceEvent();
    private ServiceParticipation serviceParticipation = new ServiceParticipation();
    private UserService serviceUser = new UserService(); // Assuming you have a ServiceUser class

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize buttons
        scanButton.setOnAction(event -> handleScanQRCode());
        closeButton.setOnAction(event -> handleClose());

        // Initialize progress bar
        scanProgressBar.setProgress(0);
        scanProgressBar.setVisible(false);
    }

    /**
     * Sets the event for scanning participation QR codes
     * @param event The current event
     */
    public void setEvent(Evenement event) {
        this.currentEvent = event;
        eventTitleLabel.setText(event.getNom_event());

        // Pour débogage
        System.out.println("Événement chargé: " + event.getNom_event() + " (ID: " + event.getId() + ")");
    }

    /**
     * Handles QR code scanning from file
     */
    private void handleScanQRCode() {
        // Create file chooser for QR code image
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un QR code");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        // Show open dialog
        File qrFile = fileChooser.showOpenDialog(scanButton.getScene().getWindow());

        if (qrFile != null) {
            try {
                // Show progress
                scanProgressBar.setVisible(true);
                scanProgressBar.setProgress(0.3);

                // Load and display the image
                BufferedImage bufferedImage = ImageIO.read(qrFile);
                Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                scannedImageView.setImage(image);

                scanProgressBar.setProgress(0.6);

                // Scan QR code
                String qrCodeContent = decodeQRCode(bufferedImage);

                if (qrCodeContent != null) {
                    // Process QR code content
                    processQRCodeContent(qrCodeContent);
                } else {
                    statusLabel.setText("QR Code invalide ou non reconnu");
                    statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    resultTextArea.setText("Aucun QR code valide détecté dans l'image.");
                }

            } catch (IOException | NotFoundException e) {
                statusLabel.setText("Erreur de lecture");
                statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                resultTextArea.setText("Erreur lors de la lecture du QR code: " + e.getMessage());
                e.printStackTrace();
            } finally {
                scanProgressBar.setProgress(1.0);
                // Hide progress after a delay
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        javafx.application.Platform.runLater(() -> {
                            scanProgressBar.setVisible(false);
                        });
                    }
                }, 800);
            }
        }
    }

    /**
     * Decodes a QR code from a BufferedImage
     * @param image The image containing the QR code
     * @return The decoded QR code text or null if not found
     * @throws NotFoundException If no QR code is found in the image
     */
    private String decodeQRCode(BufferedImage image) throws NotFoundException {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        // Try to decode the image
        try {
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException e) {
            throw e;
        }
    }

    /**
     * Processes the QR code content and validates participation
     * @param qrContent The content decoded from the QR code
     */
    private void processQRCodeContent(String qrContent) {
        // Expected format: userId:eventId:timestamp
        String[] parts = qrContent.split(":");

        if (parts.length != 3) {
            statusLabel.setText("Format QR invalide");
            statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            resultTextArea.setText("Le contenu du QR code n'est pas dans le format attendu.");
            return;
        }

        try {
            Long userId = Long.parseLong(parts[0]);
            Long eventId = Long.parseLong(parts[1]);

            // Afficher les valeurs pour le débogage
            System.out.println("QR Code - ID utilisateur: " + userId);
            System.out.println("QR Code - ID événement: " + eventId);
            System.out.println("Événement actuel - ID: " + currentEvent.getId());

            // Obtenir l'ID de l'événement actuel (sans supposer le type)
            Long currentEventId = null;
            try {
                // Si getId() renvoie un Long
                Object id = currentEvent.getId();
                if (id instanceof Long) {
                    currentEventId = (Long) id;
                } else if (id instanceof Integer) {
                    currentEventId = ((Integer) id).longValue();
                } else if (id instanceof String) {
                    currentEventId = Long.parseLong((String) id);
                } else {
                    // Si c'est un type primitif ou autre chose, nous utilisons toString et parseLong
                    currentEventId = Long.parseLong(id.toString());
                }
            } catch (Exception e) {
                System.out.println("Erreur lors de la conversion de l'ID de l'événement: " + e.getMessage());
                e.printStackTrace();
            }

            // Validate that this QR code is for the current event
            if (!eventId.equals(currentEventId)) {
                statusLabel.setText("Événement incorrect");
                statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                resultTextArea.setText("Ce QR code est pour un autre événement.\n\n" +
                        "Événement actuel: " + currentEvent.getNom_event() + "\n" +
                        "ID de l'événement actuel: " + currentEventId + "\n" +
                        "ID de l'événement du QR: " + eventId);
                return;
            }

            // Verify participation
            boolean isRegistered = serviceParticipation.participationExists(userId, eventId);

            if (isRegistered) {
                // Get user details for better display
                String userName = getUserName(userId);

                statusLabel.setText("✓ Participation valide");
                statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

                // Format participation date nicely
                Date participationDate = getParticipationDate(userId, eventId);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy à HH:mm");
                String formattedDate = participationDate != null ? dateFormat.format(participationDate) : "Date inconnue";

                resultTextArea.setText("Participant: " + userName + "\n" +
                        "ID Utilisateur: " + userId + "\n" +
                        "Événement: " + currentEvent.getNom_event() + "\n" +
                        "Date d'inscription: " + formattedDate + "\n\n" +
                        "La participation est confirmée. Le participant peut assister à l'événement.");

                // Mark attendance if needed
                // This could be implemented in a future version to mark the user as having attended

            } else {
                statusLabel.setText("⨯ Non inscrit");
                statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                resultTextArea.setText("L'utilisateur avec l'ID " + userId + " n'est pas inscrit à cet événement.\n\n" +
                        "Événement: " + currentEvent.getNom_event() + "\n\n" +
                        "Ce QR code semble avoir été généré pour un utilisateur non inscrit à l'événement.");
            }

        } catch (NumberFormatException e) {
            statusLabel.setText("Format QR invalide");
            statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            resultTextArea.setText("Le contenu du QR code contient des valeurs invalides.");
            e.printStackTrace();
        }
    }

    /**
     * Gets a user's name by their ID
     * @param userId The user ID
     * @return The user's name or a default string if not found
     */
    private String getUserName(Long userId) {
        // This is a placeholder - implement according to your user service
        // return serviceUser.getUserNameById(userId);
        return "Utilisateur #" + userId; // Temporary placeholder
    }

    /**
     * Gets the participation date for a user and event
     * @param userId The user ID
     * @param eventId The event ID
     * @return The participation date or null if not found
     */
    private Date getParticipationDate(Long userId, Long eventId) {
        // This would retrieve the actual participation date from your database
        List<Participation_event> participations = serviceParticipation.getParticipationsByUser(userId);
        for (Participation_event p : participations) {
            if (p.getEvenement_id().equals(eventId)) {
                return p.getDateparticipation();
            }
        }
        return null;
    }

    /**
     * Handles closing the scanner window
     */
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}