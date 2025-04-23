/*
package com.esprit.controllers;

import com.esprit.models.Participant;
import com.esprit.services.ParticipantService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;

public class ShowParticipantController {

    @FXML private TableView<Participant> participantTable;
    @FXML private TableColumn<Participant, Integer> idColumn;
    @FXML private TableColumn<Participant, String> nameColumn;
    @FXML private TableColumn<Participant, String> clubNameColumn;
    @FXML private TableColumn<Participant, String> statutColumn;  // Added statut column

    private final ParticipantService participantService = new ParticipantService();

    @FXML
    public void initialize() {
        // Set up table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        clubNameColumn.setCellValueFactory(new PropertyValueFactory<>("nomC")); // Assuming nomC property exists in Participant model
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut")); // Ensure statut is accessible

        loadAcceptedParticipants();
    }

    private void loadAcceptedParticipants() {
        try {
            // Fetch participants with status "accepte"
            ObservableList<Participant> acceptedParticipants = FXCollections.observableArrayList();  // Corrected to ObservableList
            participantService.getAllParticipants().forEach(participant -> {
                if ("accepte".equals(participant.getStatut())) {
                    acceptedParticipants.add(participant);  // Add participant to the list
                }
            });
            participantTable.setItems(acceptedParticipants);  // Set the items of the table
        } catch (SQLException e) {
            showError("Error loading participants: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

 */
