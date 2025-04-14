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
    private Button enable2FAButton;
    
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
        
        if (currentUser.getLastLoginAt() != null) {
            lastLoginLabel.setText("Last login: " + 
                                  currentUser.getLastLoginAt().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' hh:mm a")));
        } else {
            lastLoginLabel.setText("Last login: Not available");
        }
        
        // This would come from an actual count in a real application
        loginCountLabel.setText("Total logins: --");
        
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
            Image defaultImage = new Image(getClass().getResourceAsStream(DEFAULT_IMAGE_PATH));
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
        // Create a colored circle as default profile image (as a fallback)
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(75);
        circle.setFill(javafx.scene.paint.Color.web("#3498db")); // Blue color
        
        // Convert the circle to an image
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        
        javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(150, 150);
        circle.snapshot(params, writableImage);
        
        profileImageView.setImage(writableImage);
        
        // Add text initials if available
        if (currentUser != null && currentUser.getFirstName() != null && !currentUser.getFirstName().isEmpty()) {
            String initials = String.valueOf(currentUser.getFirstName().charAt(0));
            if (currentUser.getLastName() != null && !currentUser.getLastName().isEmpty()) {
                initials += String.valueOf(currentUser.getLastName().charAt(0));
            }
            
            // Set the initials as user data for the ImageView to be used later if needed
            profileImageView.setUserData(initials.toUpperCase());
        }
    }
    
    private void setupProfileImageHover() {
        changeImageBtn.setOnMouseEntered(e -> changeImageBtn.setOpacity(0.7));
        changeImageBtn.setOnMouseExited(e -> changeImageBtn.setOpacity(0.0));
    }
    
    private void setupPasswordFieldEvents() {
        // Real-time validation for password fields
        newPasswordField.textProperty().addListener((obs, oldText, newText) -> {
            validateNewPassword(newText);
            validateConfirmPassword();
        });
        
        confirmPasswordField.textProperty().addListener((obs, oldText, newText) -> {
            validateConfirmPassword();
        });
    }
    
    private boolean validateNewPassword(String password) {
        boolean hasLength = password.length() >= 8;
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[#?!@$%^&*-].*");
        
        // Update UI for requirements
        lengthCheckLabel.setStyle(hasLength ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        uppercaseCheckLabel.setStyle(hasUppercase ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        lowercaseCheckLabel.setStyle(hasLowercase ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        numberCheckLabel.setStyle(hasNumber ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        specialCheckLabel.setStyle(hasSpecial ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        
        return hasLength && hasUppercase && hasLowercase && hasNumber && hasSpecial;
    }
    
    private boolean validateConfirmPassword() {
        boolean matching = confirmPasswordField.getText().equals(newPasswordField.getText());
        if (!confirmPasswordField.getText().isEmpty() && !matching) {
            confirmPasswordError.setText("Passwords do not match");
            confirmPasswordError.setVisible(true);
            return false;
        } else {
            confirmPasswordError.setVisible(false);
            return true;
        }
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
            // Load the edit profile dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/edit_profile.fxml"));
            Parent root = loader.load();
            
            // Get the controller and pass the current user
            EditProfileController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            // Create a new stage for the dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Profile - UNICLUBS");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(editProfileBtn.getScene().getWindow());
            
            // Create scene without explicit dimensions - using the size defined in the FXML
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            
            // Set dimensions for the dialog
            dialogStage.setMinWidth(570);
            dialogStage.setMinHeight(670);
            dialogStage.setWidth(570);
            dialogStage.setHeight(670);
            
            // Allow resizing in case user needs to adjust the view
            dialogStage.setResizable(true);
            
            // Show the dialog and wait
            dialogStage.showAndWait();
            
            // After dialog is closed, update the profile display
            updateProfileDisplay();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Dialog Error", "Could not load edit profile dialog");
        }
    }
    
    @FXML
    private void handleChangeImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        File selectedFile = fileChooser.showOpenDialog(changeImageBtn.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Validate file size (max 5MB)
                if (selectedFile.length() > 5 * 1024 * 1024) {
                    showError("Image is too large. Maximum size is 5MB.");
                    return;
                }
                
                // Create uploads directory if it doesn't exist
                File uploadsDir = new File(UPLOADS_DIRECTORY);
                if (!uploadsDir.exists()) {
                    uploadsDir.mkdirs();
                }
                
                // Generate unique filename
                String fileName = currentUser.getId() + "_" + System.currentTimeMillis()
                        + selectedFile.getName().substring(selectedFile.getName().lastIndexOf('.'));
                
                // Copy file to uploads directory
                Path sourcePath = selectedFile.toPath();
                Path targetPath = Paths.get(UPLOADS_DIRECTORY + fileName);
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Update user profile picture in database
                currentUser.setProfilePicture(fileName);
                authService.updateUserProfile(currentUser);
                
                // Update UI
                loadProfileImage();
                
                // Show success message
                showSuccess("Profile picture updated successfully!");
                
            } catch (IOException e) {
                e.printStackTrace();
                showError("Failed to upload profile picture: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleUpdatePassword(ActionEvent event) {
        // Reset errors and messages
        clearPasswordMessages();
        
        // Get input values
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
        
        if (!validateNewPassword(newPassword)) {
            newPasswordError.setText("Password does not meet requirements");
            newPasswordError.setVisible(true);
            return;
        }
        
        // Check if new password is the same as current password
        if (currentPassword.equals(newPassword)) {
            newPasswordError.setText("New password must be different from current password");
            newPasswordError.setVisible(true);
            return;
        }
        
        // Validate password confirmation
        if (confirmPassword.isEmpty()) {
            confirmPasswordError.setText("Please confirm your password");
            confirmPasswordError.setVisible(true);
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordError.setText("Passwords do not match");
            confirmPasswordError.setVisible(true);
            return;
        }
        
        // Attempt to change password
        boolean success = authService.changePassword(currentUser.getEmail(), currentPassword, newPassword);
        
        if (success) {
            // Reset password fields
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            passwordRequirementsBox.setVisible(false);
            
            // Show success message
            passwordSuccessMessage.setText("Your password has been updated successfully!");
            passwordSuccessMessage.setVisible(true);
        } else {
            // Show error message
            passwordErrorMessage.setText("Failed to update password. Please check your current password.");
            passwordErrorMessage.setVisible(true);
        }
    }
    
    private void clearPasswordMessages() {
        currentPasswordError.setVisible(false);
        newPasswordError.setVisible(false);
        confirmPasswordError.setVisible(false);
        passwordSuccessMessage.setVisible(false);
        passwordErrorMessage.setVisible(false);
    }
    
    public void showSuccess(String message) {
        if (profileInfoMessage != null) {
            profileInfoMessage.setText(message);
            profileInfoMessage.setVisible(true);
            
            if (profileInfoError != null) {
                profileInfoError.setVisible(false);
            }
            
            // Automatically hide after 5 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    javafx.application.Platform.runLater(() -> profileInfoMessage.setVisible(false));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            // Use alert if label is not available
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
    
    public void showError(String message) {
        if (profileInfoError != null) {
            profileInfoError.setText(message);
            profileInfoError.setVisible(true);
            
            if (profileInfoMessage != null) {
                profileInfoMessage.setVisible(false);
            }
            
            // Automatically hide after 5 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    javafx.application.Platform.runLater(() -> profileInfoError.setVisible(false));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            // Use alert if label is not available
            showAlert("Error", "Error", message);
        }
    }
    
    public void updateUserData(User updatedUser) {
        // Update current user with new data
        this.currentUser = updatedUser;
        
        // Update the profile display
        updateProfileDisplay();
    }
    
    private void navigateToLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) backToDashboardButton.getScene().getWindow();
        
        // Use the utility method for consistent setup
        MainApp.setupStage(stage, root, "Login - UNICLUBS", true);
        
        stage.show();
    }
    
    @FXML
    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/admin_dashboard.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) backToDashboardButton.getScene().getWindow();
            
            // Create scene without explicit dimensions
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Admin Dashboard - UNICLUBS");
            
            // Ensure minimum dimensions for responsive layout
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            
            // Force the stage to be maximized
            stage.setMaximized(true);
            
            // Add a short delay before showing to allow layout to calculate properly
            Platform.runLater(() -> {
                // Force layout pass
                root.layout();
                
                // Trick to refresh the layout by toggling maximized state
                boolean wasMaximized = stage.isMaximized();
                stage.setMaximized(false);
                Platform.runLater(() -> {
                    stage.setMaximized(true);
                });
            });
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Navigation Error", "Could not load dashboard");
        }
    }
    
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

