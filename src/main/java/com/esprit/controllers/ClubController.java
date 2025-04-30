package com.esprit.controllers;

import com.esprit.models.Club;
import com.esprit.services.ClubService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class ClubController {

    @FXML private TextField idField;
    @FXML private TextField presidentIdField;
    @FXML private TextField nomCField;
    @FXML private TextArea descriptionField;
    @FXML private TextField statusField;
    @FXML private TextField imageField;
    @FXML private TextField pointsField;
    @FXML private ListView<Club> clubList;

    private final ClubService clubService = new ClubService();
    private final ObservableList<Club> clubs = FXCollections.observableArrayList();
    private Club selectedClub = null;

    @FXML
    public void initialize() {
        // Charger les clubs dans la ListView au démarrage
        try {
            loadClubs();

            // Configurer l'affichage personnalisé dans la ListView
            clubList.setCellFactory(param -> new ListCell<Club>() {
                @Override
                protected void updateItem(Club club, boolean empty) {
                    super.updateItem(club, empty);
                    if (empty || club == null) {
                        setText(null);
                    } else {
                        setText(club.getNomC()); // Afficher le nom du club
                    }
                }
            });

            // Écouteur pour sélectionner un club dans la ListView
            clubList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectedClub = newVal;
                    idField.setText(String.valueOf(newVal.getId()));
                    presidentIdField.setText(String.valueOf(newVal.getPresidentId()));
                    nomCField.setText(newVal.getNomC());
                    descriptionField.setText(newVal.getDescription());
                    statusField.setText(newVal.getStatus());
                    imageField.setText(newVal.getImage());
                    pointsField.setText(String.valueOf(newVal.getPoints()));
                }
            });
        } catch (SQLException e) {
            showError("Erreur lors du chargement des clubs: " + e.getMessage());
        }
    }

    private void loadClubs() throws SQLException {
        clubs.clear();
        clubs.addAll(clubService.afficher());
        clubList.setItems(clubs);
    }

    @FXML
    private void ajouterClub() {
        // Vérifier si les champs sont vides
        if (idField.getText().isEmpty() ||
                presidentIdField.getText().isEmpty() ||
                nomCField.getText().isEmpty() ||
                descriptionField.getText().isEmpty() ||
                statusField.getText().isEmpty() ||
                imageField.getText().isEmpty() ||
                pointsField.getText().isEmpty()) {

            showError("Tous les champs doivent être remplis !");
            return;
        }

        String nomC = nomCField.getText();
        String description = descriptionField.getText();

        // ✅ Bloquer les caractères spéciaux dans le nom
        if (!nomC.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showError("Le nom du club contient des caractères non autorisés.");
            return;
        }

        // ✅ Vérifier que la description ne contient pas de caractères spéciaux
        if (!description.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showError("La description contient des caractères non autorisés.");
            return;
        }

        // ✅ Vérifier la longueur de description (max 30 mots)
        if (description.trim().split("\\s+").length > 30) {
            showError("La description ne doit pas dépasser 30 mots.");
            return;
        }

        try {
            int id = Integer.parseInt(idField.getText());
            int presidentId = Integer.parseInt(presidentIdField.getText());
            String status = statusField.getText();
            String image = imageField.getText();
            int points = Integer.parseInt(pointsField.getText());

            Club club = new Club(id, presidentId, nomC, description, status, image, points);
            clubService.ajouter(club); // Ajouter le club dans la base de données
            refreshClubList(); // Rafraîchir la liste des clubs
            clearForm();

            showSuccess("Club ajouté avec succès !");
        } catch (NumberFormatException e) {
            showError("Veuillez entrer des valeurs numériques valides pour ID, président et points.");
        }
    }



    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    @FXML
    private void modifierClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club à modifier.");
            return;
        }

        try {
            selectedClub.setPresidentId(Integer.parseInt(presidentIdField.getText()));
            selectedClub.setNomC(nomCField.getText());
            selectedClub.setDescription(descriptionField.getText());
            selectedClub.setStatus(statusField.getText());
            selectedClub.setImage(imageField.getText());
            selectedClub.setPoints(Integer.parseInt(pointsField.getText()));

            clubService.modifier(selectedClub); // Mettre à jour le club dans la base de données
            refreshClubList(); // Rafraîchir la liste des clubs
            clearForm();
        } catch (NumberFormatException e) {
            showError("Veuillez entrer des valeurs valides.");
        }
    }

    @FXML
    private void supprimerClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club à supprimer.");
            return;
        }

        clubService.supprimer(selectedClub.getId()); // Supprimer le club de la base de données
        refreshClubList(); // Rafraîchir la liste des clubs
        clearForm();
    }

    @FXML
    private void accepterClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club.");
            return;
        }

        selectedClub.setStatus("Accepté");
        clubService.modifier(selectedClub); // Mettre à jour le statut
        refreshClubList(); // Rafraîchir la liste des clubs
        clearForm();
    }

    @FXML
    private void refuserClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club.");
            return;
        }

        selectedClub.setStatus("Refusé");
        clubService.modifier(selectedClub); // Mettre à jour le statut
        refreshClubList(); // Rafraîchir la liste des clubs
        clearForm();
    }

    private void refreshClubList() {
        clubs.clear();
        clubs.addAll(clubService.afficher());
        clubList.refresh();
    }

    private void clearForm() {
        idField.clear();
        presidentIdField.clear();
        nomCField.clear();
        descriptionField.clear();
        statusField.clear();
        imageField.clear();
        pointsField.clear();
        selectedClub = null;
        clubList.getSelectionModel().clearSelection();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void showClub(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/ClubView.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    public void showParticipant(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/ParticipantView.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}