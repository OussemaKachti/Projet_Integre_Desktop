package com.esprit.controllers;

import com.esprit.models.Club;
import com.esprit.services.ClubService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FormClubController {

    @FXML
    private TextField nomCField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private TextField imageField;

    private final ClubService clubService = new ClubService();
    private int presidentId = 1; // Default, will be overridden by setPresidentId

    // Set the president ID (called by ShowClubsController)
    public void setPresidentId(int presidentId) {
        this.presidentId = presidentId;
    }

    @FXML
    public void ajouterClub() {
        String nomC = nomCField.getText().trim();
        String description = descriptionField.getText().trim();
        String image = imageField.getText().trim();

        // Validation: champs vides
        if (nomC.isEmpty() || description.isEmpty() || image.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants", "Tous les champs doivent être remplis !");
            return;
        }

        // Validation: caractères spéciaux interdits
        if (!nomC.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showAlert(Alert.AlertType.WARNING, "Nom invalide", "Le nom du club contient des caractères non autorisés.");
            return;
        }

        if (!description.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showAlert(Alert.AlertType.WARNING, "Description invalide", "La description contient des caractères non autorisés.");
            return;
        }

        // Validation: description ≤ 30 mots
        if (description.split("\\s+").length > 30) {
            showAlert(Alert.AlertType.WARNING, "Description trop longue", "La description ne doit pas dépasser 30 mots.");
            return;
        }

        // Validation basique URL
        if (!image.matches("^https?://.*")) {
            showAlert(Alert.AlertType.WARNING, "URL invalide", "L'URL de l'image doit commencer par http:// ou https://");
            return;
        }

        try {
            String status = "en_attente";
            int points = 0;

            Club club = new Club(0, presidentId, nomC, description, status, image, points);
            clubService.ajouter(club);

            clearForm();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Club ajouté avec succès !");

            // Close the form
            Stage stage = (Stage) nomCField.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout du club: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearForm() {
        nomCField.clear();
        descriptionField.clear();
        imageField.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}