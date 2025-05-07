package com.esprit.controllers;

import com.esprit.models.Club;
import com.esprit.services.ClubService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ShowClubsController implements Initializable {

    @FXML
    private FlowPane clubCardContainer;

    @FXML
    private Button createClubButton;

    @FXML
    private TextField searchField;

    @FXML
    private Button prevButton;

    @FXML
    private Button nextButton;

    @FXML
    private Label pageLabel;

    private final ClubService clubService = new ClubService();
    private final int connectedUserId = 1; // Replace with actual user management
    private List<Club> allClubs; // All accepted clubs
    private List<Club> filteredClubs; // Clubs after search filter
    private int currentPage = 1;
    private final int clubsPerPage = 3;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Initialisation de ShowClubsController...");

        try {
            loadClubs();
        } catch (Exception e) {
            System.err.println("Erreur critique lors du chargement des clubs:");
            e.printStackTrace();
            showMessage("Erreur de chargement des clubs: " + e.getMessage(), "error");
        }
    }

    private void loadClubs() {
        clubCardContainer.getChildren().clear();

        allClubs = clubService.afficher().stream()
                .filter(club -> club.getStatus() != null && "accepte".equalsIgnoreCase(club.getStatus().trim()))
                .collect(Collectors.toList());

        System.out.println("Nombre total de clubs acceptés: " + allClubs.size());

        if (allClubs.isEmpty()) {
            showMessage("Aucun club accepté trouvé", "info");
            return;
        }

        filteredClubs = allClubs; // Initially, no search filter
        currentPage = 1;
        updateClubDisplay();
    }

    private void updateClubDisplay() {
        clubCardContainer.getChildren().clear();

        if (filteredClubs.isEmpty()) {
            showMessage("Aucun club correspondant trouvé", "info");
            return;
        }

        int startIndex = (currentPage - 1) * clubsPerPage;
        int endIndex = Math.min(startIndex + clubsPerPage, filteredClubs.size());

        for (int i = startIndex; i < endIndex; i++) {
            try {
                VBox card = createClubCard(filteredClubs.get(i));
                clubCardContainer.getChildren().add(card);
            } catch (Exception e) {
                System.err.println("Erreur lors de la création de la carte pour le club: " + filteredClubs.get(i).getNomC());
                e.printStackTrace();
            }
        }

        // Update pagination controls
        pageLabel.setText("Page " + currentPage);
        prevButton.setDisable(currentPage == 1);
        nextButton.setDisable(endIndex >= filteredClubs.size());
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim().toLowerCase();
        currentPage = 1; // Reset to first page on search

        if (query.isEmpty()) {
            filteredClubs = allClubs;
        } else {
            filteredClubs = allClubs.stream()
                    .filter(club -> club.getNomC().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        updateClubDisplay();
    }

    @FXML
    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updateClubDisplay();
        }
    }

    @FXML
    private void nextPage() {
        if (currentPage * clubsPerPage < filteredClubs.size()) {
            currentPage++;
            updateClubDisplay();
        }
    }

    private void showMessage(String message, String type) {
        Label label = new Label(message);
        if ("error".equals(type)) {
            label.setStyle("-fx-font-size: 16px; -fx-text-fill: red;");
        } else {
            label.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
        }
        clubCardContainer.getChildren().add(label);
    }

    private VBox createClubCard(Club club) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; " +
                "-fx-padding: 15; " +
                "-fx-border-color: #ddd; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 5, 0, 0);");

        // Nom du club
        Label nameLabel = new Label(club.getNomC());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        // Image du club
        ImageView imageView = new ImageView();
        try {
            Image image = loadImage(club.getImage());
            imageView.setImage(image);
        } catch (Exception e) {
            System.err.println("Erreur de chargement d'image pour le club: " + club.getNomC());
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/default-club.png")));
        }
        imageView.setFitWidth(220);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(false);

        // Description
        Text description = new Text(club.getDescription() != null ? club.getDescription() : "Pas de description");
        description.setWrappingWidth(220);

        // Points
        Label pointsLabel = new Label("Points: " + club.getPoints());

        // Participate button
        Button participateButton = new Button("Participate");
        participateButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white; -fx-font-weight: bold;");
        participateButton.setPrefHeight(30);
        participateButton.setPrefWidth(100);
        participateButton.setOnAction(event -> handleParticipate(club));

        card.getChildren().addAll(nameLabel, imageView, description, pointsLabel, participateButton);
        return card;
    }

    private Image loadImage(String imagePath) throws Exception {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return new Image(getClass().getResourceAsStream("/images/default-club.png"));
        }

        try {
            // Essayer comme URL
            return new Image(imagePath);
        } catch (Exception e) {
            // Si échec, essayer comme fichier local
            try {
                return new Image("file:" + imagePath);
            } catch (Exception e2) {
                System.err.println("Impossible de charger l'image depuis: " + imagePath);
                return new Image(getClass().getResourceAsStream("/images/default-club.png"));
            }
        }
    }

    private void handleParticipate(Club club) {
        try {
            // Load the participation form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/FormParticipation.fxml"));
            Parent root = loader.load();

            // Get the FormParticipationController and pass the club ID and user ID
            FormParticipationController controller = loader.getController();
            controller.setClubId(club.getId());
            controller.setUserId(connectedUserId);

            // Open the form in a new stage
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Participer au club: " + club.getNomC());
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture du formulaire de participation: " + e.getMessage());
            showMessage("Erreur lors de l'ouverture du formulaire", "error");
        }
    }

    @FXML
    private void handleCreateClub() {
        try {
            // Load the club creation form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/FormClub.fxml"));
            Parent root = loader.load();

            // Get the FormClubController and pass the connected user ID
            FormClubController controller = loader.getController();
            controller.setPresidentId(connectedUserId);

            // Open the form in a new stage
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Créer un nouveau club");
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture du formulaire de création de club: " + e.getMessage());
            showMessage("Erreur lors de l'ouverture du formulaire", "error");
        }
    }
}