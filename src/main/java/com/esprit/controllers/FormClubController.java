package com.esprit.controllers;

import com.esprit.models.Club;
import com.esprit.services.ClubService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FormClubController {

    @FXML
    private TextField nomCField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private TextField imageField;

    private final ClubService clubService = new ClubService();
    private Integer presidentId = null; // No default value; must be set by setPresidentId
    private final Connection cnx = clubService.getConnection(); // For validation

    // Set the president ID (called by ShowClubFsController)
    public void setPresidentId(int presidentId) {
        this.presidentId = presidentId;
    }

    @FXML
    public void ajouterClub() {
        // Check if presidentId was set
        if (presidentId == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "L'ID du président n'a pas été défini.");
            return;
        }

        String nomC = nomCField.getText().trim();
        String description = descriptionField.getText().trim();
        String image = imageField.getText().trim();

        if (nomC.isEmpty() || description.isEmpty() || image.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants", "Tous les champs doivent être remplis !");
            return;
        }

        if (!nomC.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showAlert(Alert.AlertType.WARNING, "Nom invalide", "Le nom du club contient des caractères non autorisés.");
            return;
        }

        if (!description.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showAlert(Alert.AlertType.WARNING, "Description invalide",
                    "La description contient des caractères non autorisés.");
            return;
        }

        if (description.split("\\s+").length > 30) {
            showAlert(Alert.AlertType.WARNING, "Description trop longue",
                    "La description ne doit pas dépasser 30 mots.");
            return;
        }

        if (!image.matches("^https?://.*")) {
            showAlert(Alert.AlertType.WARNING, "URL invalide",
                    "L'URL de l'image doit commencer par http:// ou https://");
            return;
        }

        // Validate that presidentId exists in the user table
        if (!checkPresidentExists(presidentId)) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "L'ID du président (" + presidentId + ") n'existe pas dans la table user.");
            return;
        }

        try {
            String status = "en_attente";
            int points = 0;

            Club club = new Club(0, presidentId, nomC, description, status, image, points);
            clubService.ajouter(club);

            clearForm();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Club ajouté avec succès !");

            Stage stage = (Stage) nomCField.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout du club: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to check if presidentId exists in the user table
    private boolean checkPresidentExists(int presidentId) {
        String query = "SELECT COUNT(*) FROM user WHERE id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(query)) {
            stmt.setInt(1, presidentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors de la vérification de l'ID du président: " + e.getMessage());
        }
        return false;
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