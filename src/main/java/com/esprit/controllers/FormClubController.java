package com.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.esprit.models.Club;
import com.esprit.services.ClubService;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FormClubController {

    @FXML
    private TextField nomCField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private Button chooseImageButton;

    @FXML
    private Label selectedImageLabel;

    @FXML
    private ImageView imagePreview;

    private final ClubService clubService = new ClubService();
    private Integer presidentId = null; // No default value; must be set by setPresidentId
    private final Connection cnx = clubService.getConnection(); // For validation
    private File selectedImageFile = null;
    private final String UPLOADS_DIRECTORY = "uploads/clubs/";

    // Set the president ID (called by ShowClubFsController)
    public void setPresidentId(int presidentId) {
        this.presidentId = presidentId;
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Club Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", ".png", ".jpg", ".jpeg", ".gif"));

        File selectedFile = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Validate file size (max 5MB)
                if (selectedFile.length() > 5 * 1024 * 1024) {
                    showAlert(Alert.AlertType.WARNING, "File too large", "Image must be smaller than 5MB");
                    return;
                }

                // Store the selected file
                selectedImageFile = selectedFile;
                selectedImageLabel.setText(selectedFile.getName());

                // Show image preview
                Image image = new Image(selectedFile.toURI().toString());
                imagePreview.setImage(image);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not load the selected image: " + e.getMessage());
                e.printStackTrace();
            }
        }
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

        if (nomC.isEmpty() || description.isEmpty() || selectedImageFile == null) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants",
                    "Tous les champs doivent être remplis et une image doit être sélectionnée !");
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

        // Validate that presidentId exists in the user table
        if (!checkPresidentExists(presidentId)) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "L'ID du président (" + presidentId + ") n'existe pas dans la table user.");
            return;
        }

        try {
            // Create uploads directory if it doesn't exist
            File directory = new File(UPLOADS_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate unique filename
            String fileName = "club_" + System.currentTimeMillis() +
                    selectedImageFile.getName().substring(selectedImageFile.getName().lastIndexOf("."));

            // Copy file to uploads directory
            Path sourcePath = selectedImageFile.toPath();
            Path targetPath = Paths.get(UPLOADS_DIRECTORY + fileName);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Save the file path in the database
            String imagePath = UPLOADS_DIRECTORY + fileName;

            String status = "en_attente";
            int points = 0;

            Club club = new Club(0, presidentId, nomC, description, status, imagePath, points);
            clubService.ajouter(club);

            clearForm();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Club ajouté avec succès !");

            Stage stage = (Stage) nomCField.getScene().getWindow();
            stage.close();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de fichier",
                    "Erreur lors de l'enregistrement de l'image: " + e.getMessage());
            e.printStackTrace();
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
        selectedImageFile = null;
        imagePreview.setImage(null);
        selectedImageLabel.setText("No file selected");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}