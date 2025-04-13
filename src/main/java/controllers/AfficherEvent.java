package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.Evenement;
import services.ServiceEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import utils.DataSource;

public class AfficherEvent implements Initializable {

    @FXML
    private Button addNewEventButton;

    @FXML
    private Button backButton;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryFilter;

    @FXML
    private ComboBox<String> clubFilter;

    @FXML
    private ComboBox<String> dateFilter;

    @FXML
    private FlowPane eventsContainer;

    @FXML
    private Label totalEventsLabel;

    private ServiceEvent serviceEvent;
    private List<Evenement> allEvents;




    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize service
        serviceEvent = new ServiceEvent();

        // Load events from database
        loadEvents();

        // Update total events count
        updateEventCounter();
    }
    @FXML
    private void handleAddNewEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterEvent.fxml"));
            Parent root = loader.load();
            addNewEventButton.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadEvents() {
        try {
            Connection conn = DataSource.getInstance().getCnx();
            String query = "SELECT * FROM evenement ORDER BY start_date DESC";

            PreparedStatement pst = conn.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            allEvents = new ArrayList<>();

            while (rs.next()) {
                Evenement event = new Evenement();
                event.setId(rs.getInt("id"));
                event.setNom_event(rs.getString("nom_event"));
                event.setType(rs.getString("type"));
                event.setDesc_event(rs.getString("desc_event"));
                event.setImage_description(rs.getString("image_description"));
                event.setLieux(rs.getString("lieux"));
                event.setClub_id(rs.getInt("club_id"));
                event.setCategorie_id(rs.getInt("categorie_id"));
                event.setStart_date(rs.getDate("start_date"));
                event.setEnd_date(rs.getDate("end_date"));

                allEvents.add(event);
            }

            // Display events
            displayEvents(allEvents);

        } catch (SQLException ex) {
            System.err.println("Error loading events: " + ex.getMessage());
        }
    }

    private void displayEvents(List<Evenement> events) {
        eventsContainer.getChildren().clear();

        for (Evenement event : events) {
            VBox eventCard = createEventCard(event);
            eventsContainer.getChildren().add(eventCard);
        }
    }

    private VBox createEventCard(Evenement event) {
        // Récupérer les noms du club et de la catégorie à partir des IDs
        String categoryName = serviceEvent.getCategoryNameById(event.getCategorie_id());
        String clubName = serviceEvent.getClubNameById(event.getClub_id());

        // Create main card container
        VBox eventCard = new VBox();
        eventCard.setPrefWidth(300);
        eventCard.setPrefHeight(380);
        eventCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-border-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 3);");

        // Create image container
        StackPane imageContainer = new StackPane();
        ImageView imageView = new ImageView();
        imageView.setFitHeight(180);
        imageView.setFitWidth(300);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 15 15 0 0;");

        // Try to load image from path or use default
        try {
            if (event.getImage_description() != null && !event.getImage_description().isEmpty()) {
                File imageFile = new File(event.getImage_description());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView.setImage(image);
                } else {
                    // Default image if file not found
                    imageView.setImage(new Image("/images/default-event.png"));
                }
            } else {
                // Default image if no path
                imageView.setImage(new Image("/images/default-event.png"));
            }
        } catch (Exception ex) {
            System.err.println("Error loading image: " + ex.getMessage());
            // Use default image on error
            imageView.setImage(new Image("/images/default-event.png"));
        }

        // Create status label
        Label statusLabel = new Label();

        String eventType = event.getType(); //

        statusLabel.setText(eventType);

        if ("Closed".equalsIgnoreCase(eventType)) {
            statusLabel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 5 10;");
        } else if ("Open".equalsIgnoreCase(eventType)) {
            statusLabel.setStyle("-fx-background-color: #040f71; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 5 10;");
        } else {
            statusLabel.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 5 10;");
        }

        StackPane.setAlignment(statusLabel, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(statusLabel, new javafx.geometry.Insets(10, 10, 0, 0));

        imageContainer.getChildren().addAll(imageView, statusLabel);

        // Create details container
        VBox detailsContainer = new VBox();
        detailsContainer.setStyle("-fx-padding: 15;");
        detailsContainer.setSpacing(8);

        // Category and club
        HBox categoryClubBox = new HBox();
        categoryClubBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        categoryClubBox.setSpacing(10);

        Label categoryLabel = new Label(categoryName);
        categoryLabel.setStyle("-fx-text-fill: #1e90ff;");
        categoryLabel.setFont(new javafx.scene.text.Font("Arial Bold", 12));

        Label clubLabel = new Label("• " + clubName);
        clubLabel.setStyle("-fx-text-fill: #666666;");
        clubLabel.setFont(new javafx.scene.text.Font("Arial", 12));

        categoryClubBox.getChildren().addAll(categoryLabel, clubLabel);

        // Event title
        Label titleLabel = new Label(event.getNom_event());
        titleLabel.setStyle("-fx-text-fill: #333333;");
        titleLabel.setFont(new javafx.scene.text.Font("Arial Bold", 16));

        // Event description (truncated)
        String description = event.getDesc_event();
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        Label descriptionLabel = new Label(description);
        descriptionLabel.setStyle("-fx-text-fill: #555555; -fx-wrap-text: true;");
        descriptionLabel.setFont(new javafx.scene.text.Font("Arial", 13));

        // Format dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        String dateRange;
        if (dateFormat.format(event.getStart_date()).equals(dateFormat.format(event.getEnd_date()))) {
            dateRange = dateFormat.format(event.getStart_date());
        } else {
            dateRange = dateFormat.format(event.getStart_date()) + " - " + dateFormat.format(event.getEnd_date());
        }

        // Date display
        HBox dateBox = new HBox();
        dateBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        dateBox.setSpacing(5);
        dateBox.setStyle("-fx-padding: 5 0 0 0;");

        Label dateIconLabel = new Label("📅");
        dateIconLabel.setStyle("-fx-text-fill: #666666;");
        dateIconLabel.setFont(new javafx.scene.text.Font("Arial", 13));

        Label dateTextLabel = new Label(dateRange);
        dateTextLabel.setStyle("-fx-text-fill: #666666;");
        dateTextLabel.setFont(new javafx.scene.text.Font("Arial", 13));

        dateBox.getChildren().addAll(dateIconLabel, dateTextLabel);

        // Location display
        HBox locationBox = new HBox();
        locationBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        locationBox.setSpacing(5);

        Label locationIconLabel = new Label("📍");
        locationIconLabel.setStyle("-fx-text-fill: #666666;");
        locationIconLabel.setFont(new javafx.scene.text.Font("Arial", 13));

        Label locationTextLabel = new Label(event.getLieux());
        locationTextLabel.setStyle("-fx-text-fill: #666666;");
        locationTextLabel.setFont(new javafx.scene.text.Font("Arial", 13));

        locationBox.getChildren().addAll(locationIconLabel, locationTextLabel);

        // Buttons
        HBox buttonsBox = new HBox();
        buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);
        buttonsBox.setSpacing(10);
        buttonsBox.setStyle("-fx-padding: 10 0 0 0;");

        //  bouton viewDetailsButton
        Button viewDetailsButton = new Button("View Details");
        viewDetailsButton.setStyle("-fx-background-color: #1e90ff; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 15;");
        viewDetailsButton.setFont(new javafx.scene.text.Font("Arial Bold", 13));

// Ajouter l'action pour le bouton "View Details"
        viewDetailsButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/DetailsEvent.fxml"));
                Parent root = loader.load();

                // Récupérer le contrôleur et passer l'événement sélectionné
                DetailsEvent detailsController = loader.getController();
                detailsController.setEventData(event);


                viewDetailsButton.getScene().setRoot(root);


            } catch (IOException ex) {
                System.err.println("Error loading DetailsEvent.fxml: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        Button registerButton = new Button("Register");
        registerButton.setStyle("-fx-background-color: white; -fx-text-fill: #1e90ff; -fx-background-radius: 20; -fx-border-color: #1e90ff; -fx-border-radius: 20; -fx-padding: 8 15;");
        registerButton.setFont(new javafx.scene.text.Font("Arial Bold", 13));

        // Disable registration if event is closed


        buttonsBox.getChildren().addAll(viewDetailsButton, registerButton);

        // Add all elements to details container
        detailsContainer.getChildren().addAll(
                categoryClubBox,
                titleLabel,
                descriptionLabel,
                dateBox,
                locationBox,
                buttonsBox
        );

        // Add image and details to event card
        eventCard.getChildren().addAll(imageContainer, detailsContainer);

        return eventCard;
    }

    private void updateEventCounter() {
        if (totalEventsLabel != null && allEvents != null) {
            totalEventsLabel.setText("Total Events: " + allEvents.size());
        }
    }
}