package controllers;

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
import models.Evenement;
import services.ServiceEvent;

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
    private ImageView clubLogoImageView;

    @FXML
    private Button backButton;
    @FXML
    private Button registerButton;
    @FXML
    private Button editButton;
    @FXML
    private Button shareButton;

    private Evenement currentEvent;
    private ServiceEvent serviceEvent = new ServiceEvent();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Désactiver les boutons non implémentés pour l'instant
        registerButton.setVisible(false);
        editButton.setVisible(false);
        shareButton.setVisible(false);
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

        // Charger le logo du club (utilisation d'un placeholder pour le moment)
        loadClubLogo(event.getClub_id());
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
     * Charge le logo du club
     * @param clubId l'ID du club
     */
    private void loadClubLogo(int clubId) {
        // Ici, vous pourriez ajouter une logique pour charger le logo du club depuis la base de données
        // Pour l'instant, on utilise une image par défaut
        setDefaultClubLogo();
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
     * Définit un logo par défaut pour le club
     */
    private void setDefaultClubLogo() {
        try {
            // Essayez d'abord de charger depuis les ressources
            Image defaultLogo = new Image(getClass().getResourceAsStream("/resources/images/default_club_logo.png"));
            clubLogoImageView.setImage(defaultLogo);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du logo par défaut: " + e.getMessage());

            // Si le chargement depuis les ressources échoue, essayez un chemin alternatif
            try {
                Image fallbackLogo = new Image("file:resources/images/default_club_logo.png");
                clubLogoImageView.setImage(fallbackLogo);
            } catch (Exception ex) {
                System.err.println("Impossible de charger le logo par défaut: " + ex.getMessage());
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
}