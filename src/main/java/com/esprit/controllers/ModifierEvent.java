package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import com.esprit.models.Evenement;
import com.esprit.services.ServiceEvent;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;

public class ModifierEvent implements Initializable {

    @FXML
    private TextField nom_event, lieux;

    @FXML
    private TextArea desc_event;

    @FXML
    private ComboBox<String> club_combo, categorie_combo, event_type_combo;

    @FXML
    private DatePicker start_date, end_date;

    @FXML
    private ImageView imageView;

    @FXML
    private Button chooseImageButton;

    @FXML
    private Button addEventButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button backButton; // Add reference to the back button

    private final ServiceEvent serviceEvent = new ServiceEvent();
    private String selectedImagePath;
    private Evenement currentEvent;
    private int eventId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Changer le texte du bouton pour refléter l'action de mise à jour
        addEventButton.setText("Update Event");

        // Configuration du bouton d'annulation
        cancelButton.setOnAction(event -> navigateToEventsList());

        // Configuration du bouton de retour
        if (backButton != null) {
            backButton.setOnAction(event -> navigateToEventsList());
        } else {
            System.out.println("Warning: Back button not found in the FXML");
        }

        // Charger les catégories et clubs dans les ComboBox
        loadCategories();
        loadClubs();
    }

    private void navigateToEventsList() {
        try {
            // Retourner à la page d'affichage des événements
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AfficherEvent.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page d'affichage des événements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setEventId(int id) {
        this.eventId = id;
        loadEventData(id);
    }

    private void loadEventData(int id) {
        try {
            // Récupérer l'événement depuis la base de données
            currentEvent = serviceEvent.getOne(id);

            if (currentEvent != null) {
                // Remplir les champs avec les données de l'événement
                nom_event.setText(currentEvent.getNom_event());
                desc_event.setText(currentEvent.getDesc_event());
                lieux.setText(currentEvent.getLieux());

                // Convert dates from java.util.Date to LocalDate
                if (currentEvent.getStart_date() != null) {
                    LocalDate startLocalDate = new java.sql.Date(currentEvent.getStart_date().getTime()).toLocalDate();
                    start_date.setValue(startLocalDate);
                }

                if (currentEvent.getEnd_date() != null) {
                    LocalDate endLocalDate = new java.sql.Date(currentEvent.getEnd_date().getTime()).toLocalDate();
                    end_date.setValue(endLocalDate);
                }

                // Sélectionner le club et la catégorie
                String clubName = serviceEvent.getClubNameById(currentEvent.getClub_id());
                String categoryName = serviceEvent.getCategoryNameById(currentEvent.getCategorie_id());

                club_combo.setValue(clubName);
                categorie_combo.setValue(categoryName);

                // Sélectionner le type d'événement
                event_type_combo.setValue(currentEvent.getType());

                // Charger l'image si elle existe
                if (currentEvent.getImage_description() != null && !currentEvent.getImage_description().isEmpty()) {
                    selectedImagePath = currentEvent.getImage_description();
                    File imageFile = new File(selectedImagePath);
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        imageView.setImage(image);
                    }
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Événement non trouvé");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement des données de l'événement: " + e.getMessage());
        }
    }

    private void loadCategories() {
        ObservableList<String> categories = serviceEvent.getAllCategoriesNames();
        categorie_combo.setItems(categories);
    }

    private void loadClubs() {
        ObservableList<String> clubs = serviceEvent.getAllClubsNames();
        club_combo.setItems(clubs);
    }

    @FXML
    private Button addCategoryButton;

    @FXML
    private void handleAddCategoryButton(ActionEvent event) {
        try {
            // Charger la vue d'ajout de catégorie
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AjouterCat.fxml"));
            Parent root = loader.load();

            // Créer une nouvelle scène ou utiliser une fenêtre modale
            Stage stage = new Stage();
            stage.setTitle("Ajouter une catégorie");
            stage.setScene(new Scene(root));

            // Définir le style de la fenêtre
            stage.initModality(Modality.APPLICATION_MODAL); // Empêche l'interaction avec la fenêtre principale
            stage.initOwner(addCategoryButton.getScene().getWindow());

            // Afficher la fenêtre et attendre qu'elle soit fermée
            stage.showAndWait();

            // Recharger les catégories après la fermeture de la fenêtre d'ajout
            loadCategories();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page d'ajout de catégorie: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                Image image = new Image(selectedFile.toURI().toString());
                imageView.setImage(image);
                selectedImagePath = selectedFile.getAbsolutePath(); // Stocker le chemin pour utilisation ultérieure
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement de l'image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddEvent() {
        try {
            // Vérifier que tous les champs requis sont remplis
            if (nom_event.getText().isEmpty() ||
                    desc_event.getText().isEmpty() ||
                    lieux.getText().isEmpty() ||
                    club_combo.getValue() == null ||
                    categorie_combo.getValue() == null ||
                    start_date.getValue() == null ||
                    end_date.getValue() == null) {

                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs obligatoires");
                return;
            }

            // Vérifier que la date de fin est après la date de début
            if (end_date.getValue().isBefore(start_date.getValue())) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "La date de fin doit être après la date de début");
                return;
            }

            // Récupérer les IDs depuis les noms sélectionnés
            int clubId = serviceEvent.getClubIdByName(club_combo.getValue());
            int categorieId = serviceEvent.getCategorieIdByName(categorie_combo.getValue());

            if (clubId == -1 || categorieId == -1) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Club ou catégorie non trouvé");
                return;
            }

            // Utiliser l'événement actuel pour la mise à jour
            Evenement e = currentEvent;
            e.setNom_event(nom_event.getText());
            e.setDesc_event(desc_event.getText());
            e.setLieux(lieux.getText());
            e.setClub_id(clubId);
            e.setCategorie_id(categorieId);

            // Mettre à jour le type d'événement
            if (event_type_combo.getValue() != null) {
                e.setType(event_type_combo.getValue());
            }

            // Convertir LocalDate -> java.sql.Date
            e.setStart_date(java.sql.Date.valueOf(start_date.getValue()));
            e.setEnd_date(java.sql.Date.valueOf(end_date.getValue()));

            // Gestion de l'image si une image a été sélectionnée
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                e.setImage_description(selectedImagePath);
            }

            // Mettre à jour l'événement
            serviceEvent.modifier(e);

            // Afficher une alerte de succès
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement mis à jour avec succès !");

            // Rediriger vers la page qui affiche tous les événements
            navigateToEventsList();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la mise à jour : " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}