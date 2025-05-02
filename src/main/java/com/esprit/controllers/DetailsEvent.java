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
import java.util.Date;
import java.util.ResourceBundle;
import com.esprit.models.Evenement;
import com.esprit.models.Participation_event;
import com.esprit.services.ServiceEvent;
import com.esprit.services.ServiceParticipation;
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
    private Button presidentButton1; // Delete button

    @FXML
    private Button presidentButton3; // View Participants button
    @FXML
    private Button qrCodeButton;      // Generate QR code for participants
    @FXML
    private Button scanQrCodeButton;  // Scan QR code for organizers

    private Evenement currentEvent;
    private ServiceEvent serviceEvent = new ServiceEvent();
    private ServiceParticipation serviceParticipation = new ServiceParticipation();

    // Changed from long to Long to match the required type
    private Long currentUserId = 1L; // À remplacer par l'ID de l'utilisateur connecté

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Rendre le bouton de participation visible
        registerButton.setVisible(true);
        editButton.setVisible(true);
        shareButton.setVisible(false);

        // Configure les gestionnaires d'événements
        presidentButton1.setOnAction(event -> handleDelete());
        registerButton.setOnAction(event -> handleRegister());
        presidentButton3.setOnAction(event -> handleViewParticipants());

        if (qrCodeButton != null) {
            qrCodeButton.setOnAction(event -> handleQRCode());
        }

        if (scanQrCodeButton != null) {
            scanQrCodeButton.setOnAction(event -> handleScanQRCode());
        }

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

        // Vérifier si l'utilisateur participe déjà à cet événement
        updateRegisterButtonStatus();
    }
    @FXML
    private void handleQRCode() {
        if (currentEvent == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun événement sélectionné",
                    "Impossible de générer un QR code car aucun événement n'est sélectionné.");
            return;
        }

        try {
            // Check if the user is registered first
            boolean isRegistered = serviceParticipation.participationExists(currentUserId, currentEvent.getId());

            if (!isRegistered) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Inscription requise");
                alert.setHeaderText("Inscription nécessaire");
                alert.setContentText("Vous devez vous inscrire à l'événement avant de pouvoir générer un QR code de confirmation.");
                alert.showAndWait();
                return;
            }

            // Load QR code generation view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/QRConfirmation.fxml"));
            Parent root = loader.load();

            // Get controller and set data
            QRConfirmation controller = loader.getController();
            controller.setData(currentEvent, currentUserId);

            // Create new stage for QR code window
            Stage stage = new Stage();
            stage.setTitle("QR Code de participation - " + currentEvent.getNom_event());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement",
                    "Impossible de charger l'écran de génération de QR code: " + e.getMessage());
        }
    }

    @FXML
    private void handleScanQRCode() {
        if (currentEvent == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun événement sélectionné",
                    "Impossible de scanner un QR code car aucun événement n'est sélectionné.");
            return;
        }

        try {
            // Check if current user is organizer/has permission
            // For demo purposes we'll skip this check
            // In a real app, you'd verify the user has permission to scan QR codes

            // Load QR scanner view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/QRScanner.fxml"));
            Parent root = loader.load();

            // Get controller and set data
            QRScanner controller = loader.getController();
            controller.setEvent(currentEvent);

            // Create new stage for scanner window
            Stage stage = new Stage();
            stage.setTitle("Scanner QR Code - " + currentEvent.getNom_event());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement",
                    "Impossible de charger l'écran de scan de QR code: " + e.getMessage());
        }
    }
    /**
     * Met à jour l'état du bouton d'inscription en fonction de la participation de l'utilisateur
     */
    private void updateRegisterButtonStatus() {
        if (currentEvent == null) return;

        boolean isAlreadyRegistered = serviceParticipation.participationExists(currentUserId, currentEvent.getId());

        if (isAlreadyRegistered) {
            registerButton.setText("✓ Cancel Registration");
            registerButton.setStyle("-fx-background-color: linear-gradient(to right, #e53935, #f44336); -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 15 20; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        } else {
            registerButton.setText("✓ Register for Event");
            registerButton.setStyle("-fx-background-color: linear-gradient(to right, #1976d2, #2979ff); -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 15 20; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        }
    }

    /**
     * Gère l'inscription/désinscription à un événement
     */
    @FXML
    private void handleRegister() {
        if (currentEvent == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun événement sélectionné",
                    "Impossible de s'inscrire à l'événement car aucun événement n'est sélectionné.");
            return;
        }

        // Vérifier si l'utilisateur est déjà inscrit
        boolean isAlreadyRegistered = serviceParticipation.participationExists(currentUserId, currentEvent.getId());

        if (isAlreadyRegistered) {
            // L'utilisateur est déjà inscrit, proposer d'annuler l'inscription
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Annulation d'inscription");
            confirmDialog.setHeaderText("Annuler votre inscription");
            confirmDialog.setContentText("Êtes-vous sûr de vouloir annuler votre inscription à l'événement \"" +
                    currentEvent.getNom_event() + "\" ?");

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Supprimer la participation
                boolean cancelled = serviceParticipation.annulerParticipation(currentUserId, currentEvent.getId());

                if (cancelled) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Inscription annulée",
                            "Votre inscription à l'événement a été annulée avec succès.");
                    updateRegisterButtonStatus();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'annulation",
                            "L'annulation de votre inscription a échoué. Veuillez réessayer.");
                }
            }
        } else {
            // L'utilisateur n'est pas inscrit, proposer de l'inscrire
            Date currentDate = new Date();

            // Vérifier si l'événement n'est pas déjà passé
            if (currentEvent.getEnd_date().before(currentDate)) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Événement terminé",
                        "Cet événement est déjà terminé. Vous ne pouvez plus vous y inscrire.");
                return;
            }

            // Créer une nouvelle participation
            Participation_event participation = new Participation_event();
            participation.setUser_id(currentUserId);
            participation.setEvenement_id(Long.valueOf(currentEvent.getId()));
            participation.setDateparticipation(currentDate);

            // Enregistrer la participation
            boolean registered = serviceParticipation.ajouterParticipation(participation);

            if (registered) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Inscription confirmée",
                        "Vous êtes maintenant inscrit à l'événement.");
                updateRegisterButtonStatus();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'inscription",
                        "L'inscription à l'événement a échoué. Veuillez réessayer.");
            }
        }
    }

    /**
     * Gère l'affichage de la liste des participants
     */
    @FXML
    private void handleViewParticipants() {
        if (currentEvent == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun événement sélectionné",
                    "Impossible d'afficher les participants car aucun événement n'est sélectionné.");
            return;
        }

        try {
            // Charger la vue de la liste des participants (à implémenter ultérieurement)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ParticipantsList.fxml"));
            Parent root = loader.load();

            // Passer l'ID de l'événement au contrôleur
            // ParticipantsListController controller = loader.getController();
            // controller.setEventId(currentEvent.getId());

            Stage stage = new Stage();
            stage.setTitle("Participants à l'événement " + currentEvent.getNom_event());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la liste des participants: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement",
                    "Impossible de charger la liste des participants: " + e.getMessage());
        }
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
        try {
            // Charger la vue précédente (page événements)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AfficherEvent.fxml"));
            Parent root = loader.load();

            // Obtenir le contrôleur de la page précédente si nécessaire
            // AfficherEventController controller = loader.getController();
            // controller.initialiserDonnees(...); // Si vous devez passer des données

            // Obtenir la fenêtre actuelle à partir du bouton
            Stage stage = (Stage) backButton.getScene().getWindow();

            // Définir la nouvelle scène
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Gérer l'erreur, par exemple afficher une alerte
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de Navigation");
            alert.setHeaderText("Impossible de retourner à la page précédente");
            alert.setContentText("Une erreur s'est produite: " + e.getMessage());
            alert.showAndWait();
        }
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
            modifierController.setEventId(currentEvent.getId());

            // Créer une nouvelle scène pour la vue ModifierEvent
            Stage stage = new Stage();
            stage.setTitle("Modifier l'événement");
            stage.setScene(new Scene(root));

            // Afficher la scène
            stage.show();

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