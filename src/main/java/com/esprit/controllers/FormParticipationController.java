package com.esprit.controllers;

import com.esprit.models.Participant;
import com.esprit.services.ParticipantService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class FormParticipationController {

    @FXML
    private TextArea descriptionField;

    private final ParticipantService participantService = new ParticipantService();
    private int userId;
    private int clubId;

    // Set the user ID (called by ShowClubsController)
    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Set the club ID (called by ShowClubsController)
    public void setClubId(int clubId) {
        this.clubId = clubId;
    }

    @FXML
    private void ajouterParticipant() {
        // Validate description
        String description = descriptionField.getText().trim();

        // Check if description is empty
        if (description.isEmpty()) {
            showError("La description est requise !");
            return;
        }

        // Validate description length (max 30 words)
        if (description.split("\\s+").length > 30) {
            showError("La description ne doit pas dépasser 30 mots.");
            return;
        }

        // Validate description for special characters
        if (!description.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showError("La description contient des caractères non autorisés.");
            return;
        }

        try {
            // Create and save the participant
            Participant participant = new Participant(
                    userId,
                    clubId,
                    description,
                    "en_attente" // Default status
            );
            participantService.ajouter(participant);

            showSuccess("Demande de participation envoyée avec succès !");

            // Close the form
            Stage stage = (Stage) descriptionField.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement de la participation: " + e.getMessage());
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}