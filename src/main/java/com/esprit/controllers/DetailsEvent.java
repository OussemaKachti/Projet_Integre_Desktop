package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import com.esprit.models.Evenement;
import com.esprit.services.ServiceEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class DetailsEvent implements Initializable {

    @FXML
    private Label eventTitleLabel;
    @FXML
    private Label eventTypeLabel;
    @FXML
    private Label eventCategoryLabel;
    @FXML
    private Label eventDescriptionLabel;
    @FXML
    private Label clubNameLabel;
    @FXML
    private Label startDateLabel;
    @FXML
    private Label endDateLabel;
    @FXML
    private Label locationLabel;

    @FXML
    private ImageView eventImageView;

    @FXML
    private Button backButton;
    @FXML
    private Button registerButton;
    @FXML
    private Button editButton;
    @FXML
    private Button shareButton;
    @FXML
    private Button presidentButton1; // Your delete button

    private Evenement currentEvent;
    private ServiceEvent serviceEvent = new ServiceEvent();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Désactiver les boutons non implémentés pour l'instant
        registerButton.setVisible(false);
        editButton.setVisible(true); // Modifié pour rendre le bouton Edit visible
        shareButton.setVisible(false);

        // Configure delete button handler
        presidentButton1.setOnAction(event -> handleDelete());
    }

    /**
     * Charge les données de l'événement dans la vue
     * @param event L'événement à afficher
     */
    public void setEventData(Evenement event) {
        this.currentEvent = event;

        // Définir les informations de base de l'événement
        eventTitleLabel.setText(event.getNom_event());
        eventTypeLabel.setText(event.getType());
        eventDescriptionLabel.setText(event.getDesc_event());
        locationLabel.setText(event.getLieux());

        // Formater les dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm");
        startDateLabel.setText("Du: " + dateFormat.format(event.getStart_date()));
        endDateLabel.setText("Au: " + dateFormat.format(event.getEnd_date()));

        // Obtenir et définir le nom du club
        String clubName = serviceEvent.getClubNameById(event.getClub_id());
        clubNameLabel.setText(clubName);

        // Obtenir et définir le nom de la catégorie
        String categoryName = serviceEvent.getCategoryNameById(event.getCategorie_id());
        eventCategoryLabel.setText(categoryName);

        // Charger l'image de l'événement si disponible
        loadEventImage(event.getImage_description());
    }

    /**
     * Charge l'image de l'événement
     * @param imagePath le chemin de l'image
     */
    private void loadEventImage(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    eventImageView.setImage(image);
                } else {
                    setDefaultEventImage();
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de l'image de l'événement: " + e.getMessage());
                setDefaultEventImage();
            }
        } else {
            setDefaultEventImage();
        }
    }

    /**
     * Définit une image par défaut pour l'événement
     */
    private void setDefaultEventImage() {
        try {
            // Essayez d'abord de charger depuis les ressources
            Image defaultImage = new Image(getClass().getResourceAsStream("/resources/images/default_event.png"));
            eventImageView.setImage(defaultImage);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image par défaut: " + e.getMessage());

            // Si le chargement depuis les ressources échoue, essayez un chemin alternatif
            try {
                Image fallbackImage = new Image("file:resources/images/default_event.png");
                eventImageView.setImage(fallbackImage);
            } catch (Exception ex) {
                System.err.println("Impossible de charger l'image par défaut: " + ex.getMessage());
            }
        }
    }

    /**
     * Gère l'action du bouton Retour
     */
    @FXML
    private void handleBack() {
        // Fermer la fenêtre actuelle
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Gère l'action du bouton Edit
     */
    @FXML
    private void handleEdit() {
        try {
            // Charger le FXML ModifierEvent
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ModifierEvent.fxml"));
            Parent root = loader.load();

            // Obtenir le contrôleur et lui passer l'ID de l'événement à modifier
            ModifierEvent modifierController = loader.getController();
            modifierController.setEventId(currentEvent.getId()); // Assurez-vous que cette méthode existe dans votre Evenement

            // Créer une nouvelle scène pour la vue ModifierEvent
            Stage stage = new Stage();
            stage.setTitle("Modifier l'événement");
            stage.setScene(new Scene(root));

            // Afficher la scène
            stage.show();

            // Optionnellement, fermer la fenêtre actuelle des détails
            // Stage currentStage = (Stage) editButton.getScene().getWindow();
            // currentStage.close();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la page de modification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gère l'action du bouton Delete
     */
    @FXML
    private void handleDelete() {
        if (currentEvent == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun événement à supprimer",
                    "Impossible de supprimer l'événement car aucun événement n'est sélectionné.");
            return;
        }

        // Afficher une confirmation avant de supprimer
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer l'événement");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer l'événement \"" +
                currentEvent.getNom_event() + "\" ? Cette action est irréversible.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Appeler le service pour supprimer l'événement
                boolean deleted = serviceEvent.supprimerEvenement(currentEvent.getId());

                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement supprimé",
                            "L'événement a été supprimé avec succès.");

                    // Fermer la fenêtre actuelle
                    Stage currentStage = (Stage) presidentButton1.getScene().getWindow();

                    try {
                        // Charger la page AfficherEvent
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AfficherEvent.fxml"));
                        Parent root = loader.load();

                        // Créer une nouvelle scène avec la page AfficherEvent
                        Scene scene = new Scene(root);

                        // Utiliser la même fenêtre pour afficher la nouvelle scène
                        currentStage.setScene(scene);
                        currentStage.setTitle("Liste des événements");
                        currentStage.show();

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Erreur lors du chargement de la page AfficherEvent: " + e.getMessage());
                        // En cas d'erreur, fermez simplement la fenêtre actuelle
                        currentStage.close();
                    }

                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression",
                            "La suppression de l'événement a échoué. Veuillez réessayer.");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression",
                        "Une erreur s'est produite lors de la suppression: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Affiche une boîte de dialogue d'alerte
     */
    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}