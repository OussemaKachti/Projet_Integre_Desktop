package com.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.esprit.MainApp;
import com.esprit.models.User;
import com.esprit.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private VBox profileDropdown;
    
    private User currentUser;
    
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
                        
                        // Keep aspect ratio
                        userProfilePic.setPreserveRatio(true);
                        userProfilePic.setFitHeight(40);
                        userProfilePic.setFitWidth(40);
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
    
    @FXML
    private void hideProfileDropdown() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }
    
    @FXML
    private void handleLogout() throws IOException {
        // Clear the session
        SessionManager.getInstance().clearSession();
        
        // Navigate to login page using the setupStage method for consistent sizing
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        
        // Use the utility method for consistent login screen setup
        MainApp.setupStage(stage, root, "Login - UNICLUBS", true);
    }
    
    // Navigation methods
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
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Clubs.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToEvents() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Events.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToProducts() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Products.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToCompetition() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Competition.fxml"));
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
} 