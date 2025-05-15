package com.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ResourceBundle;

import com.esprit.MainApp;
import com.esprit.models.Club;
import com.esprit.models.User;
import com.esprit.services.ClubService;
import com.esprit.services.ParticipationMembreService;
import com.esprit.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HomeController implements Initializable {

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

    private User currentUser;
    private Club userClub;
    private final ClubService clubService = new ClubService();
    private final ParticipationMembreService participationService = new ParticipationMembreService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Get current user from session
        currentUser = SessionManager.getInstance().getCurrentUser();

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
            double radius = 22.5; // Updated to match the new style
            userProfilePic.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
        }
    }

    private void loadDefaultProfilePic() {
        try {
            // Try to load from class resources
            InputStream stream = getClass().getResourceAsStream("/com/esprit/images/default-profile.png");
            
            // Check if stream is null and try alternative paths
            if (stream == null) {
                // Try different paths
                stream = getClass().getResourceAsStream("/images/default-profile.png");
                
                if (stream == null) {
                    // Last resort - create a simple default image
                    WritableImage defaultImg = new WritableImage(45, 45);
                    userProfilePic.setImage(defaultImg);
                    return;
                }
            }
            
            Image defaultImage = new Image(stream);
            userProfilePic.setImage(defaultImage);
        } catch (Exception e) {
            System.err.println("Failed to load default profile picture: " + e.getMessage());
            // Create a simple colored circle as fallback
            WritableImage defaultImg = new WritableImage(45, 45);
            userProfilePic.setImage(defaultImg);
        }
    }

    @FXML
    private void navigateToClubPolls() throws IOException {
        // Test database connection before attempting to load polls view
        try {

            // Navigate to SondageView
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/SondageView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) clubsContainer.getScene().getWindow();
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
        MainApp.setupStage(stage, root, "Login - UNICLUBS", true);
    
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
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/AfficherEvent.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) userProfileContainer.getScene().getWindow();
            
            // Replace the current scene's root instead of creating a new scene
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error navigating to events: " + e.getMessage());
            
            // Show error dialog to the user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Failed to open Events view");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
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
    private void navigateToHome() throws IOException {
        // Since we are already on the home page, we don't need to navigate
        // But we need this method to satisfy the FXML reference
    }

    @FXML
    private void navigateToPolls() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/SondageView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) userProfileContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            
            // Show error dialog to the user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Failed to open Polls view");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }
}