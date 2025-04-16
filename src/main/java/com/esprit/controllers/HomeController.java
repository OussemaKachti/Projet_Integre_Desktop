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
            double radius = 20;
            userProfilePic.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
            
            // Initially hide the dropdown
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
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
    
    @FXML
    private void showProfileDropdown() {
        profileDropdown.setVisible(true);
        profileDropdown.setManaged(true);
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
        
        // Navigate to login page
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
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