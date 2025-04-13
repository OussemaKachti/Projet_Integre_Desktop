package controllers;

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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import models.Evenement;
import services.ServiceEvent;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import javafx.fxml.Initializable;
import java.net.URL;
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

    private final ServiceEvent serviceEvent = new ServiceEvent();
    private String selectedImagePath;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Charger les catégories et clubs dans les ComboBox
        loadCategories();
        loadClubs();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterCat.fxml"));
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

            Evenement e = new Evenement();
            e.setNom_event(nom_event.getText());
            e.setDesc_event(desc_event.getText());
            e.setLieux(lieux.getText());
            e.setClub_id(clubId);
            e.setCategorie_id(categorieId);

            // Ajout de cette ligne pour définir le type
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherEvent.fxml"));
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