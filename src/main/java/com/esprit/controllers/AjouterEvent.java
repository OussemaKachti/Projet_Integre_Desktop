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

import javafx.fxml.Initializable;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AjouterEvent implements Initializable {

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
    private Button addCategoryButton;

    private final ServiceEvent serviceEvent = new ServiceEvent();
    private String selectedImagePath;

    // Constants for validation
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MIN_DESC_LENGTH = 10;
    private static final int MAX_DESC_LENGTH = 500;
    private static final int MIN_DAYS_IN_FUTURE = 1; // Minimum days in the future for start date
    private static final int MAX_EVENT_DURATION_DAYS = 30; // Maximum duration of an event in days

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Charger les catégories et clubs dans les ComboBox
        loadCategories();
        loadClubs();

        // Initialize date pickers with validators
        setupDateValidation();

        // Set up text field validation listeners
        setupTextFieldValidation();
    }

    private void setupDateValidation() {
        // Set default values to help users
        start_date.setValue(LocalDate.now().plusDays(1));
        end_date.setValue(LocalDate.now().plusDays(2));

        // Add listeners to validate dates when they change
        start_date.valueProperty().addListener((obs, oldVal, newVal) -> {
            validateDates();
        });

        end_date.valueProperty().addListener((obs, oldVal, newVal) -> {
            validateDates();
        });
    }

    private void validateDates() {
        if (start_date.getValue() != null && end_date.getValue() != null) {
            LocalDate today = LocalDate.now();

            // Apply visual styling based on validation
            if (start_date.getValue().isBefore(today)) {
                applyErrorStyle(start_date);
            } else {
                removeErrorStyle(start_date);
            }

            if (end_date.getValue().isBefore(start_date.getValue())) {
                applyErrorStyle(end_date);
            } else {
                removeErrorStyle(end_date);
            }
        }
    }

    private void setupTextFieldValidation() {
        // Real-time validation for event name
        nom_event.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() < MIN_NAME_LENGTH || newVal.length() > MAX_NAME_LENGTH) {
                applyErrorStyle(nom_event);
            } else {
                removeErrorStyle(nom_event);
            }
        });

        // Real-time validation for event description
        desc_event.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() < MIN_DESC_LENGTH || newVal.length() > MAX_DESC_LENGTH) {
                applyErrorStyle(desc_event);
            } else {
                removeErrorStyle(desc_event);
            }
        });
    }

    private void applyErrorStyle(Control control) {
        control.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
    }

    private void removeErrorStyle(Control control) {
        control.setStyle("");
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
        // Réinitialiser les styles d'erreur
        removeErrorStyle(nom_event);
        removeErrorStyle(desc_event);
        removeErrorStyle(lieux);
        removeErrorStyle(start_date);
        removeErrorStyle(end_date);

        // Valider les champs obligatoires
        StringBuilder errorMessages = new StringBuilder();
        boolean hasErrors = false;

        // Validation du nom
        String nomValue = nom_event.getText().trim();
        if (nomValue.isEmpty()) {
            applyErrorStyle(nom_event);
            errorMessages.append("- Le nom de l'événement est obligatoire.\n");
            hasErrors = true;
        } else if (nomValue.length() < MIN_NAME_LENGTH) {
            applyErrorStyle(nom_event);
            errorMessages.append("- Le nom doit contenir au moins " + MIN_NAME_LENGTH + " caractères.\n");
            hasErrors = true;
        } else if (nomValue.length() > MAX_NAME_LENGTH) {
            applyErrorStyle(nom_event);
            errorMessages.append("- Le nom ne peut pas dépasser " + MAX_NAME_LENGTH + " caractères.\n");
            hasErrors = true;
        }

        // Validation de la description
        String descValue = desc_event.getText().trim();
        if (descValue.isEmpty()) {
            applyErrorStyle(desc_event);
            errorMessages.append("- La description de l'événement est obligatoire.\n");
            hasErrors = true;
        } else if (descValue.length() < MIN_DESC_LENGTH) {
            applyErrorStyle(desc_event);
            errorMessages.append("- La description doit contenir au moins " + MIN_DESC_LENGTH + " caractères.\n");
            hasErrors = true;
        } else if (descValue.length() > MAX_DESC_LENGTH) {
            applyErrorStyle(desc_event);
            errorMessages.append("- La description ne peut pas dépasser " + MAX_DESC_LENGTH + " caractères.\n");
            hasErrors = true;
        }

        // Validation du lieu
        if (lieux.getText().trim().isEmpty()) {
            applyErrorStyle(lieux);
            errorMessages.append("- Le lieu de l'événement est obligatoire.\n");
            hasErrors = true;
        }

        // Validation des sélections dans les ComboBox
        if (club_combo.getValue() == null) {
            applyErrorStyle(club_combo);
            errorMessages.append("- Veuillez sélectionner un club.\n");
            hasErrors = true;
        }

        if (categorie_combo.getValue() == null) {
            applyErrorStyle(categorie_combo);
            errorMessages.append("- Veuillez sélectionner une catégorie.\n");
            hasErrors = true;
        }

        // Validation des dates
        LocalDate today = LocalDate.now();
        if (start_date.getValue() == null) {
            applyErrorStyle(start_date);
            errorMessages.append("- La date de début est obligatoire.\n");
            hasErrors = true;
        } else if (start_date.getValue().isBefore(today)) {
            applyErrorStyle(start_date);
            errorMessages.append("- La date de début doit être dans le futur.\n");
            hasErrors = true;
        }

        if (end_date.getValue() == null) {
            applyErrorStyle(end_date);
            errorMessages.append("- La date de fin est obligatoire.\n");
            hasErrors = true;
        } else if (start_date.getValue() != null && end_date.getValue().isBefore(start_date.getValue())) {
            applyErrorStyle(end_date);
            errorMessages.append("- La date de fin doit être après la date de début.\n");
            hasErrors = true;
        }

        // Vérification de la durée de l'événement
        if (start_date.getValue() != null && end_date.getValue() != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start_date.getValue(), end_date.getValue());
            if (daysBetween > MAX_EVENT_DURATION_DAYS) {
                applyErrorStyle(end_date);
                errorMessages.append("- La durée de l'événement ne peut pas dépasser " + MAX_EVENT_DURATION_DAYS + " jours.\n");
                hasErrors = true;
            }
        }

        // Si des erreurs ont été trouvées, afficher un message et ne pas continuer
        if (hasErrors) {
            showAlert(Alert.AlertType.ERROR, "Erreurs de validation", errorMessages.toString());
            return;
        }

        try {
            // Récupérer les IDs depuis les noms sélectionnés
            int clubId = serviceEvent.getClubIdByName(club_combo.getValue());
            int categorieId = serviceEvent.getCategorieIdByName(categorie_combo.getValue());

            if (clubId == -1 || categorieId == -1) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Club ou catégorie non trouvé");
                return;
            }

            Evenement e = new Evenement();
            e.setNom_event(nomValue);
            e.setDesc_event(descValue);
            e.setLieux(lieux.getText().trim());
            e.setClub_id(clubId);
            e.setCategorie_id(categorieId);

            // Ajout du type d'événement
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

            // Ajouter l'événement
            serviceEvent.ajouter(e);

            // Afficher une alerte de succès
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement ajouté avec succès !");

            // Rediriger vers la page qui affiche tous les événements
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AfficherEvent.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) addEventButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page de liste des événements: " + ex.getMessage());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'ajout : " + ex.getMessage());
        }
    }

    // Méthode pour réinitialiser les champs
    private void clearFields() {
        nom_event.clear();
        desc_event.clear();
        lieux.clear();
        club_combo.setValue(null);
        categorie_combo.setValue(null);
        start_date.setValue(null);
        end_date.setValue(null);
        imageView.setImage(null);
        selectedImagePath = null;
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}