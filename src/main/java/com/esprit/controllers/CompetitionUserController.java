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
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CompetitionUserController implements Initializable {
    
    @FXML
    private StackPane userProfileContainer;
    
    @FXML
    private ImageView userProfilePic;
    
    @FXML
    private Label userNameLabel;
    
    @FXML
    private VBox profileDropdown;
    
    @FXML
    private VBox competitionsContainer;
    
    @FXML
    private VBox pastCompetitionsContainer;
    
    @FXML
    private Label noCompetitionsLabel;
    
    @FXML
    private Label noPastCompetitionsLabel;
    
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
            
            // Load competitions (this would be implemented to fetch from a service)
            loadCompetitions();
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
    
    private void loadCompetitions() {
        // This method would load competitions from a service
        // For now, we'll just show the "no competitions" labels
        noCompetitionsLabel.setVisible(true);
        noPastCompetitionsLabel.setVisible(true);
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
    private void joinCompetition() {
        showAlert("Competition", "Join Competition", "Competition registration is not yet available. Check back soon!");
    }
    
    @FXML
    private void handleLogout() throws IOException {
        // Clear the session
        SessionManager.getInstance().clearSession();
        
        // Navigate to login page
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/esprit/views/login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }
    
    // Navigation methods
    @FXML
    private void navigateToHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/esprit/views/home.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }
    
    @FXML
    private void navigateToProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/esprit/views/profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToClubs() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/esprit/views/Clubs.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToEvents() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/esprit/views/Events.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToProducts() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/esprit/views/Products.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToContact() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/esprit/views/Contact.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }
    
    /**
     * Shows an alert dialog with the specified title, header, and content text.
     * This method is reused across the application for consistent alert display.
     * 
     * @param title The title of the alert dialog
     * @param header The header text for the alert dialog
     * @param content The content text for the alert dialog
     */
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        // Apply custom styling using our CSS
        URL cssUrl = getClass().getResource("/com/esprit/styles/competition.css");
        if (cssUrl != null) {
            String css = cssUrl.toExternalForm();
            Scene scene = alert.getDialogPane().getScene();
            scene.getStylesheets().add(css);
        }
        
        alert.showAndWait();
    }
} 