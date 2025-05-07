package com.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
<<<<<<< HEAD
=======
import java.sql.SQLException;
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
import java.util.ResourceBundle;

import com.esprit.MainApp;
import com.esprit.models.User;
<<<<<<< HEAD
import com.esprit.utils.SessionManager;
=======
import com.esprit.models.Club;
import com.esprit.models.ParticipationMembre;
import com.esprit.services.ClubService;
import com.esprit.services.ParticipationMembreService;
import com.esprit.utils.SessionManager;
import com.esprit.utils.DataSource;
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
<<<<<<< HEAD
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
=======
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HomeController implements Initializable {
<<<<<<< HEAD
    
    @FXML
    private StackPane userProfileContainer;
    
    @FXML
    private ImageView userProfilePic;
    
    @FXML
    private Label userNameLabel;
    
    @FXML
    private VBox profileDropdown;
    
    private User currentUser;
    
=======

    @FXML
    private StackPane userProfileContainer;

    @FXML
    private ImageView userProfilePic;

    @FXML
    private Label userNameLabel;

    @FXML
    private VBox profileDropdown;

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

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Get current user from session
        currentUser = SessionManager.getInstance().getCurrentUser();
<<<<<<< HEAD
        
        if (currentUser != null) {
            // Set user name
            userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
            
=======

        if (currentUser != null) {
            // Set user name
            userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
            // Load profile picture
            String profilePicture = currentUser.getProfilePicture();
            if (profilePicture != null && !profilePicture.isEmpty()) {
                try {
                    File imageFile = new File("uploads/profiles/" + profilePicture);
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        userProfilePic.setImage(image);
<<<<<<< HEAD
                        
                        // Keep aspect ratio
                        userProfilePic.setPreserveRatio(true);
                        userProfilePic.setFitHeight(40);
                        userProfilePic.setFitWidth(40);
=======
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
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
<<<<<<< HEAD
            
            // Apply circular clip to profile picture
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(20, 20, 20);
            userProfilePic.setClip(clip);
            
            // Ensure dropdown is properly positioned and hidden initially
            javafx.application.Platform.runLater(() -> {
                profileDropdown.setVisible(false);
                profileDropdown.setManaged(false);
                profileDropdown.toFront();
                
                // Position the dropdown properly
                userProfileContainer.layout(); // Force layout to calculate proper size
            });
        }
    }
    
    private void loadDefaultProfilePic() {
        try {
            // Use the correct filename that exists in the resources
            Image defaultImage = new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile-png.png"));
            if (defaultImage == null || defaultImage.isError()) {
                // Try alternative paths
                try {
                    defaultImage = new Image(getClass().getClassLoader().getResourceAsStream("com/esprit/images/default-profile-png.png"));
                } catch (Exception e) {
                    // If that fails, try another common path
                    try {
                        File fallbackFile = new File("src/main/resources/com/esprit/images/default-profile-png.png");
                        if (fallbackFile.exists()) {
                            defaultImage = new Image(fallbackFile.toURI().toString());
                        } else {
                            // If all resource paths fail, create a colored circle as fallback
                            createDefaultImageFallback();
                            return;
                        }
                    } catch (Exception ex) {
                        // If all resource paths fail, create a colored circle as fallback
                        createDefaultImageFallback();
                        return;
                    }
                }
            }
            
            if (defaultImage != null && !defaultImage.isError()) {
                userProfilePic.setImage(defaultImage);
                userProfilePic.setPreserveRatio(true);
                userProfilePic.setFitHeight(40);
                userProfilePic.setFitWidth(40);
            } else {
                // If loading fails, create a colored circle as fallback
                createDefaultImageFallback();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Create a colored circle as fallback
            createDefaultImageFallback();
        }
    }
    
    private void createDefaultImageFallback() {
        // Create a colored circle as default profile image
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(20);
        circle.setFill(javafx.scene.paint.Color.web("#00A0E3")); // UNICLUBS blue

        // Convert the circle to an image
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(40, 40);
        circle.snapshot(params, writableImage);

        userProfilePic.setImage(writableImage);

        // Add text initials if available
        if (currentUser != null && currentUser.getFirstName() != null && !currentUser.getFirstName().isEmpty()) {
            String initials = String.valueOf(currentUser.getFirstName().charAt(0));
            if (currentUser.getLastName() != null && !currentUser.getLastName().isEmpty()) {
                initials += String.valueOf(currentUser.getLastName().charAt(0));
            }
            
            // Store initials as user data for potential future use
            userProfilePic.setUserData(initials.toUpperCase());
        }
    }
    
    @FXML
    private void showProfileDropdown() {
        // Ensure dropdown is on top of other elements
        profileDropdown.toFront();
        profileDropdown.setMouseTransparent(false);
        
        // Make it visible
        profileDropdown.setVisible(true);
        profileDropdown.setManaged(true);
        profileDropdown.setOpacity(1.0);
    }
    
=======

            // Apply circular clip to profile picture
            double radius = 22.5; // Updated to match the new style
            userProfilePic.setClip(new javafx.scene.shape.Circle(radius, radius, radius));

            // Initially hide the dropdowns
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
            clubsDropdown.setVisible(false);
            clubsDropdown.setManaged(false);

            // Check if user is a club member or president and setup club dropdown
            setupClubsDropdown();
        }
    }

    /**
     * Checks if the current user is a member or president of a club
     * and sets up the club dropdown accordingly
     */
    private void setupClubsDropdown() {
        // Get all clubs that the user is a member of
        boolean isMemberOfAnyClub = false;

        try {
            // Check if the user is a president of any club
            userClub = clubService.findByPresident(currentUser.getId());
            if (userClub != null) {
                isMemberOfAnyClub = true;
            } else {
                // Check if user is a member of any club
                isMemberOfAnyClub = !participationService.getParticipationsByClub(currentUser.getId()).isEmpty();
            }
        } catch (SQLException e) {
            System.err.println("Error checking club membership: " + e.getMessage());
        }

        // For simplicity, we keep the clubs dropdown visible regardless of membership
        // In a production app, you might want to hide it if the user isn't a member of
        // any club
    }

    private void loadDefaultProfilePic() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile.png"));
            userProfilePic.setImage(defaultImage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showProfileDropdown() {
        profileDropdown.setVisible(true);
        profileDropdown.setManaged(true);
    }

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
    @FXML
    private void hideProfileDropdown() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }
<<<<<<< HEAD
    
=======

    @FXML
    private void showClubsDropdown() {
        clubsDropdown.setVisible(true);
        clubsDropdown.setManaged(true);
        clubsDropdown.toFront();
    }

    @FXML
    private void hideClubsDropdown() {
        clubsDropdown.setVisible(false);
        clubsDropdown.setManaged(false);
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

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
    @FXML
    private void handleLogout() throws IOException {
        // Clear the session
        SessionManager.getInstance().clearSession();
<<<<<<< HEAD
        
        // Navigate to login page using the setupStage method for consistent sizing
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        
        // Use the utility method for consistent login screen setup
        MainApp.setupStage(stage, root, "Login - UNICLUBS", true);
    }
    
    // Navigation methods
=======

        // Navigate to login page
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
    @FXML
    private void navigateToProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }
<<<<<<< HEAD
    
=======

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
    @FXML
    private void navigateToSettings() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Settings.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToClubs() throws IOException {
<<<<<<< HEAD
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Clubs.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
=======
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/ShowClubs.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) clubsContainer.getScene().getWindow();
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToEvents() throws IOException {
<<<<<<< HEAD
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Events.fxml"));
=======
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AfficherEvent.fxml"));
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToProducts() throws IOException {
<<<<<<< HEAD
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Products.fxml"));
=======
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/produit/ProduitView.fxml"));
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToCompetition() throws IOException {
<<<<<<< HEAD
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Competition.fxml"));
=======
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/UserCompetition.fxml"));
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
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
<<<<<<< HEAD
} 
=======

    @FXML
    private void navigateToHome() throws IOException {
        // Since we are already on the home page, we don't need to navigate
        // But we need this method to satisfy the FXML reference
    }
}
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
