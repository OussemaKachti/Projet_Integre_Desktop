package com.esprit.controllers;

import com.esprit.models.Evenement;
import com.esprit.models.User;
import com.esprit.services.ServiceParticipation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.List;

public class ParticipantsListController {

    @FXML
    private Label eventNameLabel;

    @FXML
    private Label participantCountLabel;

    @FXML
    private TableView<User> participantsTable;

    @FXML
    private TableColumn<User, String> fullNameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, LocalDateTime> registrationDateColumn; // Changé à LocalDateTime

    @FXML
    private Button closeButton;

    private Evenement currentEvent;
    private ServiceParticipation serviceParticipation = new ServiceParticipation();

    @FXML
    private void initialize() {
        System.out.println("Initialisation du contrôleur ParticipantsList");

        // Configure les colonnes de la table
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Utiliser lastLoginAt comme substitut temporaire pour la date d'inscription
        registrationDateColumn.setCellValueFactory(new PropertyValueFactory<>("lastLoginAt"));

        // Configurer le bouton de fermeture
        closeButton.setOnAction(event -> {
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
        });
    }

    /**
     * Initialise les données de la vue avec l'événement sélectionné
     * @param event L'événement dont on veut afficher les participants
     */
    public void initData(Evenement event) {
        System.out.println("Méthode initData appelée avec événement: " + event.getNom_event());
        this.currentEvent = event;

        // Mettre à jour les labels avec les informations de l'événement
        eventNameLabel.setText(event.getNom_event());

        System.out.println("ID de l'événement passé à initData: " + event.getId());

        // Charger la liste des participants
        loadParticipants();
    }

    /**
     * Charge la liste des participants pour l'événement actuel
     */
    private void loadParticipants() {
        if (currentEvent == null) {
            System.out.println("Erreur: currentEvent est null");
            return;
        }

        System.out.println("Chargement des participants pour l'événement ID: " + currentEvent.getId());

        // Récupérer les participants de l'événement
        List<User> participants = serviceParticipation.getParticipantsByEvent((long) currentEvent.getId());

        // Déboguer pour voir si des participants sont renvoyés
        System.out.println("Nombre de participants récupérés: " + participants.size());
        if (participants.isEmpty()) {
            System.out.println("Aucun participant trouvé pour cet événement");
        } else {
            System.out.println("Premier participant: " + participants.get(0).toString());
        }

        // Mettre à jour le compteur de participants
        participantCountLabel.setText("Total Participants: " + participants.size());

        // Afficher les participants dans la table
        ObservableList<User> participantsList = FXCollections.observableArrayList(participants);
        participantsTable.setItems(participantsList);

        // Forcer le rafraîchissement de la table
        participantsTable.refresh();
    }
}