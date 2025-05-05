package com.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;

import com.esprit.MainApp;
import com.esprit.models.User;
import com.esprit.services.AuthService;
import com.esprit.utils.SessionManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AdminProfileController {

    @FXML
    private ImageView profileImageView;
    
    @FXML
    private Label usernameLabel;
    
    @FXML
    private Label userRoleLabel;
    
    @FXML
    private Label userStatusLabel;
    
    @FXML
    private Label nameValueLabel;
    
    @FXML
    private Label emailValueLabel;
    
    @FXML
    private Label phoneValueLabel;
    
    @FXML
    private Label accountCreatedLabel;
    
    @FXML
    private Label lastLoginLabel;
    
    @FXML
    private Label loginCountLabel;
    
    @FXML
    private Button editProfileBtn;
    
    @FXML
    private Button changeImageBtn;
    
    @FXML
    private Button logoutButton;
    
    @FXML
    private Button backToDashboardButton;
    
    @FXML
    private Label profileInfoMessage;
    
    @FXML
    private Label profileInfoError;
    
    // Password change fields
    @FXML
    private PasswordField currentPasswordField;
    
    @FXML
    private PasswordField newPasswordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label currentPasswordError;
    
    @FXML
    private Label newPasswordError;
    
    @FXML
    private Label confirmPasswordError;
    
    @FXML
    private Label passwordSuccessMessage;
    
    @FXML
    private Label passwordErrorMessage;
    
    @FXML
    private VBox passwordRequirementsBox;
    
    @FXML
    private Label lengthCheckLabel;
    
    @FXML
    private Label uppercaseCheckLabel;
    
    @FXML
    private Label lowercaseCheckLabel;
    
    @FXML
    private Label numberCheckLabel;
    
    @FXML
    private Label specialCheckLabel;
    
    private final AuthService authService = new AuthService();
    private User currentUser;
    private final String UPLOADS_DIRECTORY = "uploads/profiles/";
    private final String DEFAULT_IMAGE_PATH = "/images/default_profile.png";
    
    @FXML
    private void initialize() {
        // Set back to dashboard button action
        backToDashboardButton.setOnAction(event -> navigateToDashboard());
        
        // Load current admin user
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Redirect to login if not logged in
            try {
                navigateToLogin();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Session Error", "Could not redirect to login page");
            }
        }
        
        // Check if the user is an admin
        if (!"ADMINISTRATEUR".equals(currentUser.getRole().toString())) {
            try {
                navigateToLogin();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Access Denied", "You do not have permission to access the admin profile");
            }
        }
        
        // Update profile information
        updateProfileDisplay();
        
        // Setup profile image hover effect
        setupProfileImageHover();
        
        // Setup password field events
        setupPasswordFieldEvents();
    }
    
    private void updateProfileDisplay() {
        // Update header info
        String fullName = currentUser.getFirstName() + " " + currentUser.getLastName();
        usernameLabel.setText(fullName);
        userRoleLabel.setText(currentUser.getRole().toString());
        
        // Update profile details
        nameValueLabel.setText(fullName);
        emailValueLabel.setText(currentUser.getEmail());
        phoneValueLabel.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "Not provided");
        
        // Format dates if available
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        if (currentUser.getCreatedAt() != null) {
            accountCreatedLabel.setText(currentUser.getCreatedAt().format(formatter));
        } else {
            accountCreatedLabel.setText("Not available");
        }
        
        // We need to check if loginCountLabel and lastLoginLabel exist
        // since we removed the security tab that contained them
        if (lastLoginLabel != null) {
            if (currentUser.getLastLoginAt() != null) {
                lastLoginLabel.setText("Last login: " + 
                                    currentUser.getLastLoginAt().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' hh:mm a")));
            } else {
                lastLoginLabel.setText("Last login: Not available");
            }
        }
        
        // Only update loginCountLabel if it exists
        if (loginCountLabel != null) {
            // This would come from an actual count in a real application
            loginCountLabel.setText("Total logins: --");
        }
        
        // Load profile image if exists
        loadProfileImage();
    }
    
    private void loadProfileImage() {
        String profilePicture = currentUser.getProfilePicture();
        
        try {
            if (profilePicture != null && !profilePicture.isEmpty()) {
                File imageFile = new File(UPLOADS_DIRECTORY + profilePicture);
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    profileImageView.setImage(image);
                } else {
                    // Use a default image if profile picture file not found
                    loadDefaultImage();
                }
            } else {
                // Use default image if no profile picture set
                loadDefaultImage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Create a fallback colored circle if image loading fails
            createDefaultImageFallback();
        }
        
        // Apply circular clip to the image view
        profileImageView.setStyle("-fx-background-radius: 75; -fx-background-color: #cccccc;");
    }
    
    private void loadDefaultImage() {
        try {
            // First try to load the default image from resources
            Image defaultImage = new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile-png.png"));
            if (defaultImage != null && !defaultImage.isError()) {
                profileImageView.setImage(defaultImage);
            } else {
                // If default image couldn't be loaded, create a fallback image
                createDefaultImageFallback();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // If any error occurs, create a fallback image
            createDefaultImageFallback();
        }
    }
    
    private void createDefaultImageFallback() {
        // Create a simple colored background with initials
        try {
            String initials = "";
            if (currentUser.getFirstName() != null && !currentUser.getFirstName().isEmpty()) {
                initials += currentUser.getFirstName().charAt(0);
            }
            if (currentUser.getLastName() != null && !currentUser.getLastName().isEmpty()) {
                initials += currentUser.getLastName().charAt(0);
            }
            
            // Set a default background for the image view
            profileImageView.setStyle("-fx-background-color: #3498db; -fx-background-radius: 75;");
            
            // Set image to null to show the background
            profileImageView.setImage(null);
            
            // TODO: Add initials text to the center of the circle (Would need to use Canvas or a custom solution)
            
        } catch (Exception e) {
            e.printStackTrace();
            // If all else fails, just show a plain circle
            profileImageView.setStyle("-fx-background-color: #cccccc; -fx-background-radius: 75;");
            profileImageView.setImage(null);
        }
    }
    
    private void setupProfileImageHover() {
        changeImageBtn.setOnMouseEntered(e -> changeImageBtn.setOpacity(0.8));
        changeImageBtn.setOnMouseExited(e -> changeImageBtn.setOpacity(0.0));
    }
    
    private void setupPasswordFieldEvents() {
        // Set up real-time validation for password fields
        newPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateNewPassword(newValue);
        });
        
        confirmPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateConfirmPassword();
        });
    }
    
    private boolean validateNewPassword(String password) {
        boolean isValid = true;
        
        // Check password length (8+ characters)
        boolean hasValidLength = password.length() >= 8;
        lengthCheckLabel.setStyle(hasValidLength ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        
        // Check for uppercase letter
        boolean hasUppercase = !password.equals(password.toLowerCase());
        uppercaseCheckLabel.setStyle(hasUppercase ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        
        // Check for lowercase letter
        boolean hasLowercase = !password.equals(password.toUpperCase());
        lowercaseCheckLabel.setStyle(hasLowercase ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        
        // Check for number
        boolean hasNumber = password.matches(".*\\d.*");
        numberCheckLabel.setStyle(hasNumber ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        
        // Check for special character
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
        specialCheckLabel.setStyle(hasSpecial ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        
        return hasValidLength && hasUppercase && hasLowercase && hasNumber && hasSpecial;
    }
    
    private boolean validateConfirmPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        boolean isMatch = newPassword.equals(confirmPassword);
        
        if (!isMatch && !confirmPassword.isEmpty()) {
            confirmPasswordError.setText("Passwords do not match");
            confirmPasswordError.setVisible(true);
        } else {
            confirmPasswordError.setVisible(false);
        }
        
        return isMatch;
    }
    
    @FXML
    private void handleLogout(ActionEvent event) {
        // Clear session
        SessionManager.getInstance().clearSession();
        
        // Navigate to login
        try {
            navigateToLogin();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Logout Error", "Failed to navigate to login page");
        }
    }
    
    @FXML
    private void handleEditProfile(ActionEvent event) {
        try {
            // Load the edit profile view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/edit_profile.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the current user
            EditProfileController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            // No parent controller needed as we'll update ourselves
            // controller.setParentController(this);
            
            // Create a new stage for the edit profile dialog
            Stage stage = new Stage();
            stage.setTitle("Edit Profile");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            // Set the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            
            // Apply stylesheet
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());
            
            // Show the dialog
            stage.showAndWait();
            
            // After dialog is closed, we need to update our display
            updateProfileDisplay();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Profile Error", "Could not open edit profile dialog");
        }
    }
    
    @FXML
    private void handleChangeImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        // Show file chooser dialog
        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        
        if (selectedFile != null) {
            try {
                // Create uploads directory if it doesn't exist
                File directory = new File(UPLOADS_DIRECTORY);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                
                // Generate unique filename
                String fileName = currentUser.getId() + "_" + System.currentTimeMillis() + 
                                 selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                
                // Copy file to uploads directory
                Path sourcePath = selectedFile.toPath();
                Path targetPath = Paths.get(UPLOADS_DIRECTORY + fileName);
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Update user profile picture in database
                currentUser.setProfilePicture(fileName);
                boolean updateSuccess = authService.updateUserProfile(currentUser);
                
                if (updateSuccess) {
                    // Update image in UI
                    Image image = new Image(targetPath.toUri().toString());
                    profileImageView.setImage(image);
                    
                    // Show success message
                    showSuccess("Profile picture updated successfully");
                } else {
                    showError("Failed to update profile picture in database");
                }
                
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "File Error", "Could not save the profile image: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleUpdatePassword(ActionEvent event) {
        // Clear previous messages
        clearPasswordMessages();
        
        // Get password values
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate current password
        if (currentPassword.isEmpty()) {
            currentPasswordError.setText("Current password is required");
            currentPasswordError.setVisible(true);
            return;
        }
        
        // Validate new password
        if (newPassword.isEmpty()) {
            newPasswordError.setText("New password is required");
            newPasswordError.setVisible(true);
            return;
        }
        
        // Check if new password meets requirements
        if (!validateNewPassword(newPassword)) {
            newPasswordError.setText("Password doesn't meet all requirements");
            newPasswordError.setVisible(true);
            return;
        }
        
        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            confirmPasswordError.setText("Please confirm your new password");
            confirmPasswordError.setVisible(true);
            return;
        }
        
        // Check if passwords match
        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordError.setText("Passwords do not match");
            confirmPasswordError.setVisible(true);
            return;
        }
        
        // Verify current password
        User authenticatedUser = authService.authenticate(currentUser.getEmail(), currentPassword);
        
        if (authenticatedUser == null) {
            // Current password is incorrect
            currentPasswordError.setText("Current password is incorrect");
            currentPasswordError.setVisible(true);
            return;
        }
        
        // Update password
        try {
            boolean updateSuccess = authService.changePassword(currentUser.getEmail(), currentPassword, newPassword);
            
            if (updateSuccess) {
                // Clear password fields
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
                
                // Show success message
                passwordSuccessMessage.setVisible(true);
            } else {
                passwordErrorMessage.setText("Failed to update password. Please try again.");
                passwordErrorMessage.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            passwordErrorMessage.setText("Error: " + e.getMessage());
            passwordErrorMessage.setVisible(true);
        }
    }
    
    private void clearPasswordMessages() {
        // Clear all error messages
        currentPasswordError.setVisible(false);
        newPasswordError.setVisible(false);
        confirmPasswordError.setVisible(false);
        passwordSuccessMessage.setVisible(false);
        passwordErrorMessage.setVisible(false);
    }
    
    public void showSuccess(String message) {
        // Hide error message if visible
        profileInfoError.setVisible(false);
        
        // Show success message
        profileInfoMessage.setText(message);
        profileInfoMessage.setVisible(true);
        
        // Fade out after 5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    profileInfoMessage.setVisible(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    public void showError(String message) {
        // Hide success message if visible
        profileInfoMessage.setVisible(false);
        
        // Show error message
        profileInfoError.setText(message);
        profileInfoError.setVisible(true);
        
        // Fade out after 5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    profileInfoError.setVisible(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    public void updateUserData(User updatedUser) {
        if (updatedUser != null) {
            this.currentUser = updatedUser;
            updateProfileDisplay();
        }
    }
    
     private void navigateToLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) userRoleLabel.getScene().getWindow();
        
        // Use the utility method for consistent setup
        MainApp.setupStage(stage, root, "Login - UNICLUBS", true);
    }
    
    @FXML
    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/admin_dashboard.fxml"));
            Parent root = loader.load();
            
            // Get the current stage
            Stage currentStage = (Stage) backToDashboardButton.getScene().getWindow();
            
            // Create a new stage completely
            Stage newStage = new Stage();
            newStage.setTitle("Admin Dashboard - UNICLUBS");
            
            // Create scene with initial size
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());
            
            // Set scene to stage
            newStage.setScene(scene);
            
            // Set maximized state BEFORE showing
            newStage.setMaximized(true);
            
            // Show the new stage first
            newStage.show();
            
            // Then close the current stage
            currentStage.close();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Navigation Error", "Failed to navigate to admin dashboard");
        }
    }
    
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

