package com.esprit.controllers;

import com.esprit.models.Participant;
import com.esprit.services.ParticipantService;
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

public class ParticipantController {

    @FXML private TextField userIdField;
    @FXML private TextField clubIdField;
    @FXML private TextField descriptionField;
    @FXML private ComboBox<String> statutBox;
    @FXML private ListView<Participant> participantList;

    private final ParticipantService participantService = new ParticipantService();
    private final ObservableList<Participant> participants = FXCollections.observableArrayList();
    private Participant selectedParticipant = null;

    @FXML
    public void initialize() {
        // Initialiser la ComboBox avec les options de statut
        statutBox.setItems(FXCollections.observableArrayList("en_attente", "accepté", "refusé"));

        // Charger les participants dans la ListView au démarrage
        try {
            loadParticipants();

            // Configurer l'affichage personnalisé dans la ListView
            participantList.setCellFactory(param -> new ListCell<Participant>() {
                @Override
                protected void updateItem(Participant participant, boolean empty) {
                    super.updateItem(participant, empty);
                    if (empty || participant == null) {
                        setText(null);
                    } else {
                        setText("ID: " + participant.getUser_id() + " | Club ID: " + participant.getClub_id() + " | Statut: " + participant.getStatut());
                    }
                }
            });

            // Écouteur pour sélectionner un participant dans la ListView
            participantList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectedParticipant = newVal;
                    userIdField.setText(String.valueOf(newVal.getUser_id()));
                    clubIdField.setText(String.valueOf(newVal.getClub_id()));
                    descriptionField.setText(newVal.getDescription());
                    statutBox.setValue(newVal.getStatut());
                }
            });
        } catch (SQLException e) {
            showError("Erreur lors du chargement des participants: " + e.getMessage());
        }
    }

    private void loadParticipants() throws SQLException {
        participants.clear();
        participants.addAll(participantService.afficher());
        participantList.setItems(participants);
    }

    @FXML
    private void ajouterParticipant() {
        // Vérification des champs vides
        if (userIdField.getText().isEmpty() ||
                clubIdField.getText().isEmpty() ||
                descriptionField.getText().isEmpty() ||
                statutBox.getValue() == null || statutBox.getValue().isEmpty()) {

            showError("Tous les champs doivent être remplis !");
            return;
        }

        String description = descriptionField.getText();

        // ✅ Vérifier la longueur de description (max 30 mots)
        if (description.trim().split("\\s+").length > 30) {
            showError("La description ne doit pas dépasser 30 mots.");
            return;
        }

        // ✅ Vérifier que la description ne contient pas de caractères spéciaux
        if (!description.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showError("La description contient des caractères non autorisés.");
            return;
        }

        try {
            int userId = Integer.parseInt(userIdField.getText());
            int clubId = Integer.parseInt(clubIdField.getText());
            String status = statutBox.getValue();

            Participant participant = new Participant(userId, clubId, description, status);
            participantService.ajouter(participant); // Ajouter le participant dans la base de données
            refreshParticipantList(); // Rafraîchir la liste des participants
            clearForm();

            showSuccess("Participant ajouté avec succès !");
        } catch (NumberFormatException e) {
            showError("Veuillez entrer des valeurs numériques valides pour les IDs.");
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
    private void modifierParticipant() {
        if (selectedParticipant == null) {
            showError("Veuillez sélectionner un participant à modifier.");
            return;
        }

        try {
            selectedParticipant.setUser_id(Integer.parseInt(userIdField.getText()));
            selectedParticipant.setClub_id(Integer.parseInt(clubIdField.getText()));
            selectedParticipant.setDescription(descriptionField.getText());
            selectedParticipant.setStatut(statutBox.getValue());

            participantService.modifier(selectedParticipant); // Mettre à jour le participant dans la base de données
            refreshParticipantList(); // Rafraîchir la liste des participants
            clearForm();
        } catch (NumberFormatException e) {
            showError("Veuillez entrer des valeurs valides pour les IDs.");
        }
    }

    @FXML
    private void supprimerParticipant() {
        if (selectedParticipant == null) {
            showError("Veuillez sélectionner un participant à supprimer.");
            return;
        }

        participantService.supprimer(selectedParticipant.getUser_id()); // Supprimer le participant de la base de données
        refreshParticipantList(); // Rafraîchir la liste des participants
        clearForm();
    }

    private void refreshParticipantList() {
        participants.clear();
        participants.addAll(participantService.afficher());
        participantList.refresh();
    }

    private void clearForm() {
        userIdField.clear();
        clubIdField.clear();
        descriptionField.clear();
        statutBox.setValue(null);
        selectedParticipant = null;
        participantList.getSelectionModel().clearSelection();
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