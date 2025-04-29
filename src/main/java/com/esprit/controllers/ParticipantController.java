package com.esprit.controllers;

import com.esprit.models.Club;
import com.esprit.models.Participant;
import com.esprit.services.ClubService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParticipantController {

    @FXML private TextField userIdField;
    @FXML private TextField clubIdField;
    @FXML private TextField clubNameField;
    @FXML private TextField descriptionField;
    @FXML private TextField statutField;
    @FXML private TextField searchField;
    @FXML private ListView<Participant> participantList;

    private final ParticipantService participantService = new ParticipantService();
    private final ClubService clubService = new ClubService();
    private final ObservableList<Participant> participants = FXCollections.observableArrayList();
    private List<Participant> allParticipants; // To store the full list for filtering
    private Map<Integer, String> clubIdToNameMap = new HashMap<>(); // Cache for club ID to name mapping
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
            // Charger les noms des clubs pour le mapping
            loadClubNames();

            // Charger les participants
            loadParticipants();
            allParticipants = participantService.afficher(); // Store the full list for filtering

            // Configurer l'affichage personnalisé dans la ListView
            participantList.setCellFactory(param -> new ListCell<Participant>() {
                @Override
                protected void updateItem(Participant participant, boolean empty) {
                    super.updateItem(participant, empty);
                    if (empty || participant == null) {
                        setText(null);
                    } else {
                        String clubName = clubIdToNameMap.getOrDefault(participant.getClub_id(), "Inconnu");
                        setText("Utilisateur: " + participant.getUser_id() + " | Club: " + clubName + " | Statut: " + participant.getStatut());
                    }
                }
            });

            // Écouteur pour sélectionner un participant dans la ListView
            participantList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectedParticipant = newVal;
                    userIdField.setText(String.valueOf(newVal.getUser_id()));
                    clubIdField.setText(String.valueOf(newVal.getClub_id()));
                    clubNameField.setText(clubIdToNameMap.getOrDefault(newVal.getClub_id(), "Inconnu"));
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

    // Charger les noms des clubs pour le mapping
    private void loadClubNames() throws SQLException {
        List<Club> clubs = clubService.afficher();
        for (Club club : clubs) {
            clubIdToNameMap.put(club.getId(), club.getNomC());
        }
    }

    // Method to set club ID and prefill the form
    public void setClubId(int clubId) {
        clubIdField.setText(String.valueOf(clubId));
        clubIdField.setEditable(false); // Prevent editing club ID

        // Mettre à jour le champ du nom du club
        clubNameField.setText(clubIdToNameMap.getOrDefault(clubId, "Inconnu"));

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

    @FXML
    private void searchParticipants() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            participants.setAll(allParticipants);
        } else {
            List<Participant> filteredParticipants = allParticipants.stream()
                    .filter(participant -> {
                        String clubName = clubIdToNameMap.getOrDefault(participant.getClub_id(), "Inconnu").toLowerCase();
                        return clubName.contains(searchText);
                    })
                    .collect(Collectors.toList());
            participants.setAll(filteredParticipants);
        }
        participantList.setItems(participants);
    }

    private void refreshParticipantList() throws SQLException {
        participants.clear();
        allParticipants = participantService.afficher();
        participants.addAll(allParticipants);
        participantList.refresh();
    }

    private void clearForm() {
        userIdField.setText(String.valueOf(currentUserId)); // Reset to current user ID
        clubIdField.clear();
        clubNameField.clear();
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
