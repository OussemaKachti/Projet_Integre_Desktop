package com.esprit.controllers;

import com.esprit.models.ParticipationMembre;
import com.esprit.services.AiMService;
import com.esprit.services.ParticipationMembreService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class FormParticipationController {

    @FXML
    private TextArea descriptionField;

    private final ParticipationMembreService participantService = new ParticipationMembreService();
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
        String description = descriptionField.getText().trim();

        if (description.isEmpty()) {
            showError("La description est requise !");
            return;
        }

        if (description.split("\\s+").length > 30) {
            showError("La description ne doit pas dépasser 30 mots.");
            return;
        }

        if (!description.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showError("La description contient des caractères non autorisés.");
            return;
        }

        try {
            // 🔥 NEW PART: Check with AI before adding participant
            if (AiMService.containsBadWords(description)) {
                showError("Votre description contient des mots inappropriés. Veuillez corriger et réessayer.");
                return;
            }

            // If everything is fine ➔ Add participant
            ParticipationMembre participant = new ParticipationMembre(
                    userId,
                    clubId,
                    description,
                    "en_attente" // Default status
            );
            participantService.ajouter(participant);

            showSuccess("Demande de participation envoyée avec succès !");
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