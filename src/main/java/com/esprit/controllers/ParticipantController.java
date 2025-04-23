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
    @FXML private TextField statutField;
    @FXML private ListView<Participant> participantList;

    private final ParticipantService participantService = new ParticipantService();
    private final ObservableList<Participant> participants = FXCollections.observableArrayList();
    private Participant selectedParticipant = null;

    // Assuming a logged-in user (placeholder values)
    private final int currentUserId = 1; // Replace with actual user ID from user management
    private final String currentUserName = "Current User"; // Replace with actual user name

    @FXML
    public void initialize() {
        // Préremplir l'ID utilisateur
        userIdField.setText(String.valueOf(currentUserId));
        userIdField.setEditable(false); // Prevent editing user ID

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
                    statutField.setText(newVal.getStatut());
                } else {
                    clearForm();
                }
            });
        } catch (SQLException e) {
            showError("Erreur lors du chargement des participants: " + e.getMessage());
        }
    }

    // Method to set club ID and prefill the form
    public void setClubId(int clubId) {
        clubIdField.setText(String.valueOf(clubId));
        clubIdField.setEditable(false); // Prevent editing club ID

        // Automatically save participation request with default values
        try {
            Participant participant = new Participant(
                    currentUserId,
                    clubId,
                    "Demande de participation de " + currentUserName, // Default description
                    "en_attente" // Default status
            );
            participantService.ajouter(participant);
            showSuccess("Demande de participation enregistrée automatiquement !");
            refreshParticipantList();
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement automatique de la participation: " + e.getMessage());
        }
    }

    private void loadParticipants() throws SQLException {
        participants.clear();
        participants.addAll(participantService.afficher());
        participantList.setItems(participants);
    }

    @FXML
    private void accepterParticipant() {
        if (selectedParticipant == null) {
            showError("Veuillez sélectionner un participant à accepter.");
            return;
        }

        try {
            selectedParticipant.setStatut("accepte");
            participantService.modifier(selectedParticipant); // Mettre à jour le participant dans la base de données
            refreshParticipantList(); // Rafraîchir la liste des participants
            clearForm();
            showSuccess("Participant accepté avec succès !");
        } catch (Exception e) {
            showError("Erreur lors de l'acceptation du participant: " + e.getMessage());
        }
    }

    @FXML
    private void supprimerParticipant() {
        if (selectedParticipant == null) {
            showError("Veuillez sélectionner un participant à supprimer.");
            return;
        }

        try {
            // Use the participant's ID (primary key) to delete the record
            boolean deleted = participantService.supprimer(selectedParticipant.getId());
            if (deleted) {
                refreshParticipantList(); // Rafraîchir la liste des participants
                clearForm();
                showSuccess("Participant supprimé avec succès !");
            } else {
                showError("Échec de la suppression : le participant n'a pas été trouvé dans la base de données.");
            }
        } catch (SQLException e) {
            showError("Erreur lors de la suppression du participant: " + e.getMessage());
        }
    }

    private void refreshParticipantList() {
        participants.clear();
        participants.addAll(participantService.afficher());
        participantList.setItems(participants); // Ensure the ListView is updated
        participantList.refresh();
    }

    private void clearForm() {
        userIdField.setText(String.valueOf(currentUserId)); // Reset to current user ID
        clubIdField.clear();
        descriptionField.clear();
        statutField.clear();
        selectedParticipant = null;
        participantList.getSelectionModel().clearSelection();
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