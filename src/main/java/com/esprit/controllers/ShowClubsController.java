package com.esprit.controllers;

import com.esprit.MainApp;
import com.esprit.models.Club;
import com.esprit.models.User;
import com.esprit.services.ClubService;
import com.esprit.utils.SessionManager; // Use SessionManager instead of UserSession
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
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

    @FXML
    private StackPane userProfileContainer;

    @FXML
    private ImageView userProfilePic;

    @FXML
    private Label userNameLabel;

    @FXML
    private StackPane clubsContainer;

    @FXML
    private Button clubsButton;

    @FXML
    private VBox clubsDropdown;

    private final ClubService clubService = new ClubService();
    private final int connectedUserId = getCurrentUserId(); // Updated to get from SessionManager
    private List<Club> allClubs; // All accepted clubs
    private List<Club> filteredClubs; // Clubs after search filter
    private int currentPage = 1;
    private final int clubsPerPage = 3;
    private Club userClub;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Initialisation de ShowClubsController...");

        // Get current user from session
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Set user name
            userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());

            // Load profile picture
            String profilePicture = currentUser.getProfilePicture();
            if (profilePicture != null && !profilePicture.isEmpty()) {
                try {
                    File imageFile = new File("uploads/profiles/" + profilePicture);
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        userProfilePic.setImage(image);
                    } else {
                        loadDefaultProfilePic();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    loadDefaultProfilePic();
                }
            } else {
                loadDefaultProfilePic();
            }

            // Apply circular clip to profile picture
            double radius = 22.5;
            userProfilePic.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
        }

        try {
            loadClubs();
        } catch (Exception e) {
            System.err.println("Erreur critique lors du chargement des clubs:");
            e.printStackTrace();
            showMessage("Erreur de chargement des clubs: " + e.getMessage(), "error");
        }
    }

    private void loadDefaultProfilePic() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile.png"));
            userProfilePic.setImage(defaultImage);
        } catch (Exception e) {
            e.printStackTrace();
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
                System.err.println(
                        "Erreur lors de la création de la carte pour le club: " + filteredClubs.get(i).getNomC());
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
        VBox card = new VBox(15);
        card.setPrefWidth(300);
        card.setPrefHeight(380);
        card.getStyleClass().add("club-card");
        
        // Club Image
        ImageView imageView = new ImageView();
        try {
            Image image = loadImage(club.getImage());
            imageView.setImage(image);
        } catch (Exception e) {
            System.err.println("Erreur de chargement d'image pour le club: " + club.getNomC());
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/default-club.png")));
        }
        imageView.setFitWidth(300);
        imageView.setFitHeight(180);
        imageView.getStyleClass().add("club-image");
        
        // Club Content Container
        VBox contentBox = new VBox(12);
        contentBox.getStyleClass().add("club-content");
        contentBox.setPadding(new Insets(15));
        
        // Club Name with Trophy Icon for Points
        HBox namePointsBox = new HBox(10);
        namePointsBox.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(club.getNomC());
        nameLabel.getStyleClass().add("club-name");
        
        HBox pointsBox = new HBox(5);
        pointsBox.setAlignment(Pos.CENTER);
        pointsBox.getStyleClass().add("points-container");
        Label trophyIcon = new Label("🏆");
        Label pointsLabel = new Label(String.valueOf(club.getPoints()));
        pointsBox.getChildren().addAll(trophyIcon, pointsLabel);
        
        namePointsBox.getChildren().addAll(nameLabel, pointsBox);
        
        // Club Description
        Text description = new Text(club.getDescription() != null ? club.getDescription() : "Pas de description");
        description.getStyleClass().add("club-description");
        description.setWrappingWidth(270);
        
        // President Info
        HBox presidentBox = new HBox(10);
        presidentBox.setAlignment(Pos.CENTER_LEFT);
        presidentBox.getStyleClass().add("president-info");
        
        Label presidentIcon = new Label("👤");
        Label presidentLabel = new Label("President: " + 
            (club.getPresident() != null ? 
             club.getPresident().getFirstName() + " " + club.getPresident().getLastName() : 
             "Non assigné"));
        presidentBox.getChildren().addAll(presidentIcon, presidentLabel);
        
        // Status Badge
        Label statusBadge = new Label(club.getStatus() != null ? club.getStatus().toUpperCase() : "N/A");
        statusBadge.getStyleClass().addAll("status-badge", "status-" + (club.getStatus() != null ? club.getStatus().toLowerCase() : "unknown"));
        
        // Participate Button
        Button participateButton = new Button("Rejoindre le Club");
        participateButton.getStyleClass().add("participate-button");
        participateButton.setPrefWidth(270);
        participateButton.setOnAction(event -> handleParticipate(club));
        
        // Add all elements to the content box
        contentBox.getChildren().addAll(
            namePointsBox,
            description,
            presidentBox,
            statusBadge,
            participateButton
        );
        
        // Add all elements to the main card
        card.getChildren().addAll(imageView, contentBox);
        
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

    // Helper method to get the current user's ID from SessionManager
    private int getCurrentUserId() {
        SessionManager session = SessionManager.getInstance();
        if (session.getCurrentUser() == null) {
            System.err.println(
                    "Aucun utilisateur connecté détecté dans SessionManager. Utilisation de l'ID par défaut: 1");
            showAlert("Erreur", "Aucun utilisateur connecté détecté. Veuillez vous connecter.");
            return 1; // Default fallback (should be replaced with proper navigation to login)
        }
        int userId = session.getCurrentUser().getId();
        System.out.println("Utilisateur connecté détecté avec ID: " + userId);
        return userId;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void navigateToHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Home.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToClubPolls() throws IOException {
        // Test database connection before attempting to load polls view
        try {

            // Navigate to SondageView
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/SondageView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) userProfileContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            // Handle any other exceptions that might occur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Failed to open Polls view");
            alert.setContentText("An error occurred while trying to open the Polls view: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void navigateToEvents() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AfficherEvent.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToProducts() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/produit/ProduitView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToCompetition() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/UserCompetition.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToContact() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Contact.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void viewEventDetails() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/EventDetails.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToSettings() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Settings.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToClubs() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/ShowClubs.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) clubsContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToMyClub() throws IOException {
        if (userClub != null) {
            // Navigate to the user's club page
            // Since ClubDetailsController might not exist or have the expected method,
            // we'll just navigate to clubs for now
            navigateToClubs();
        } else {
            // User doesn't have a club, navigate to clubs list
            navigateToClubs();
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        // Clear the session
        SessionManager.getInstance().clearSession();

        // Navigate to login page
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        
        // Use the utility method for consistent setup
        MainApp.setupStage(stage, root, "Login - UNICLUBS",true);
    }
}