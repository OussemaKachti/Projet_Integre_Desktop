// Path: src/main/java/controllers/ProfileController.java
package com.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.esprit.MainApp;
import com.esprit.models.User;
import com.esprit.services.AuthService;
import com.esprit.services.EmailService;
import com.esprit.utils.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ProfileController {

    @FXML
    private ImageView profileImageView;

    @FXML
    private Label usernameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Label nameValueLabel;

    @FXML
    private Label emailValueLabel;

    @FXML
    private Label phoneValueLabel;

    @FXML
    private Button editProfileBtn;

    @FXML
    private Button changeImageBtn;

    @FXML
    private Button logoutButton;
    
    @FXML
    private Button dashboardButton;

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
    private final EmailService emailService = new EmailService();
    private User currentUser;
    private final String UPLOADS_DIRECTORY = "uploads/profiles/";
    private final String DEFAULT_IMAGE_PATH = "/com/esprit/images/default-profile-png.png";

    @FXML
    private void initialize() {
        // Load current user
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Redirect to login if not logged in
            try {
                navigateToLogin();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Set dashboard button visibility based on role
        if (dashboardButton != null) {
            dashboardButton.setVisible("ADMIN".equals(currentUser.getRole().toString()));
            dashboardButton.setManaged("ADMIN".equals(currentUser.getRole().toString()));
        }

        // Update profile information
        updateProfileDisplay();

        // Setup profile image hover effect
        setupProfileImageHover();

        // Setup password field events
        setupPasswordFieldEvents();
    }

    private void updateProfileDisplay() {
        // Update header and sidebar info
        String fullName = currentUser.getFirstName() + " " + currentUser.getLastName();
        usernameLabel.setText(fullName);
        userRoleLabel.setText(currentUser.getRole().toString());

        // Update profile details
        nameValueLabel.setText(fullName);
        emailValueLabel.setText(currentUser.getEmail());
        phoneValueLabel.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "Not provided");

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
        profileImageView.setStyle("-fx-background-radius: 60; -fx-background-color: #cccccc;");
    }

    private void loadDefaultImage() {
        try {
            // First try to load the default image from resources
            Image defaultImage = new Image(getClass().getResourceAsStream(DEFAULT_IMAGE_PATH));
            if (defaultImage != null && !defaultImage.isError()) {
                profileImageView.setImage(defaultImage);
            } else {
                // Try the alternative path format
                defaultImage = new Image(DEFAULT_IMAGE_PATH);
                if (defaultImage != null && !defaultImage.isError()) {
                    profileImageView.setImage(defaultImage);
                } else {
                    // If default image couldn't be loaded, create a fallback image
                    createDefaultImageFallback();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // If any error occurs, create a fallback image
            createDefaultImageFallback();
        }
    }

    private void createDefaultImageFallback() {
        // Create a colored circle as default profile image (as a fallback)
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(60);
        circle.setFill(javafx.scene.paint.Color.web("#00A0E3")); // UNICLUBS blue

        // Convert the circle to an image
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.image.WritableImage writableImage
                = new javafx.scene.image.WritableImage(120, 120);
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

        // Show password requirements when password field is focused
        newPasswordField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            passwordRequirementsBox.setVisible(newValue);
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
    public void handleLogout(ActionEvent event) {
        // Clear session
        SessionManager.getInstance().clearSession();

        // Navigate to login
        try {
            navigateToLogin();
        } catch (IOException e) {
            e.printStackTrace();
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
            controller.setParentController(this);
            
            // Create a new stage for the dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Profile - UNICLUBS");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(editProfileBtn.getScene().getWindow());
            
            // Create scene without explicit dimensions - using the size defined in the FXML
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            
            // Increase dimensions to ensure all content (including buttons) is visible
            dialogStage.setMinWidth(570);
            dialogStage.setMinHeight(670); // Increased height to ensure buttons are visible
            dialogStage.setWidth(570);
            dialogStage.setHeight(670); // Increased height to ensure buttons are visible
            
            // Allow resizing in case user needs to adjust the view
            dialogStage.setResizable(true);
            
            // Center the dialog on screen for more reliable positioning
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            dialogStage.setX((screenBounds.getWidth() - dialogStage.getWidth()) / 2);
            dialogStage.setY((screenBounds.getHeight() - dialogStage.getHeight()) / 2);
            
            // Show the dialog and wait
            dialogStage.showAndWait(); 
        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not load edit profile dialog: " + e.getMessage());
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
                // Clear warning tracking to ensure new validations can trigger warnings
                authService.clearWarningTracking();
                
                // Validate file size (max 5MB)
                if (selectedFile.length() > 5 * 1024 * 1024) {
                    showError("Image is too large. Maximum size is 5MB.");
                    return;
                }

                // Show loading indicator
                showLoading("Validating image content with AI...");
                changeImageBtn.setDisable(true);
                
                // Create a unique file for this validation attempt to prevent caching issues
                File tempFile = new File(selectedFile.getAbsolutePath() + "_" + System.currentTimeMillis());
                Files.copy(selectedFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                com.esprit.utils.AiContentValidator.validateImageAsync(tempFile, new com.esprit.utils.AiContentValidator.ValidationCallback() {
                    @Override
                    public void onValidationComplete(boolean isValid, String message) {
                        // Delete temp file
                        tempFile.delete();
                        
                        // Re-enable button
                        changeImageBtn.setDisable(false);
                        
                        if (!isValid) {
                            // Get the image caption from the AiValidationService result
                            String imageCaption = null;
                            if (com.esprit.utils.AiContentValidator.getLastValidationResult() != null) {
                                imageCaption = com.esprit.utils.AiContentValidator.getLastValidationResult().getImageCaption();
                            }
                            
                            // Record the inappropriate content warning with the caption
                            authService.addContentWarning(currentUser, "profile image", imageCaption);
                            
                            // Log the profanity incident for admin dashboard visibility
                            com.esprit.utils.ProfanityLogManager.logProfanityIncident(
                                currentUser, 
                                "profile image", 
                                imageCaption != null ? imageCaption : "Inappropriate image content", 
                                "High", 
                                "Profile image rejected"
                            );
                            
                            // Refresh current user to get updated warning count
                            currentUser = authService.findUserByEmail(currentUser.getEmail());
                            
                            // Check if account has been deactivated (3+ warnings)
                            if (currentUser.getWarningCount() >= 3) {
                                // Display account suspension message
                                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                                alert.setTitle("Account Suspended");
                                alert.setHeaderText("Your account has been deactivated");
                                alert.setContentText("Due to repeated content policy violations, your account has been suspended. You will now be logged out. Please contact support for assistance.");
                                
                                // Ensure dialog is properly sized and resizable
                                alert.getDialogPane().setPrefWidth(500);
                                alert.getDialogPane().setPrefHeight(200);
                                alert.setResizable(true);
                                
                                alert.showAndWait();
                                
                                // Logout the user and redirect to login page
                                SessionManager.getInstance().clearSession();
                                try {
                                    navigateToLogin();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    showError("Error navigating to login: " + e.getMessage());
                                }
                                return;
                            }
                            
                            // Show error message
                            showError(message + " This violation has been recorded.");
                            return;
                        }
                        
                        // Proceed with upload if valid
                        try {
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
                });
            } catch (Exception e) {
                e.printStackTrace();
                changeImageBtn.setDisable(false);
                showError("Failed to process profile picture: " + e.getMessage());
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
        try {
            boolean updateSuccess = authService.changePassword(currentUser.getEmail(), currentPassword, newPassword);
            
            if (updateSuccess) {
                // Clear password fields
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
                
                // Show success message
                passwordSuccessMessage.setVisible(true);
                
                // Send password change notification email
                String fullName = currentUser.getFirstName() + " " + currentUser.getLastName();
                emailService.sendPasswordChangeNotificationAsync(
                    currentUser.getEmail(),
                    fullName
                );
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
        currentPasswordError.setVisible(false);
        newPasswordError.setVisible(false);
        confirmPasswordError.setVisible(false);
        passwordSuccessMessage.setVisible(false);
        passwordErrorMessage.setVisible(false);
    }

    public void showSuccess(String message) {
        profileInfoMessage.setText(message);
        profileInfoMessage.setVisible(true);
        profileInfoError.setVisible(false);

        // Automatically hide after 5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                javafx.application.Platform.runLater(() -> profileInfoMessage.setVisible(false));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void showError(String message) {
        profileInfoError.setText(message);
        profileInfoError.setVisible(true);
        profileInfoMessage.setVisible(false);

        // Automatically hide after 5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                javafx.application.Platform.runLater(() -> profileInfoError.setVisible(false));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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
        
        Stage stage = (Stage) userRoleLabel.getScene().getWindow();
        
        // Use the utility method for consistent setup
        MainApp.setupStage(stage, root, "Login - UNICLUBS", true);
    }

    @FXML
    public void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/dashboard.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) dashboardButton.getScene().getWindow();
            // ... existing code ...
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void navigateToHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Home.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    /**
     * Shows an informational message
     */
    public void showInfo(String message) {
        profileInfoMessage.setText(message);
        profileInfoMessage.setVisible(true);
        profileInfoError.setVisible(false);
    }

    /**
     * Shows a loading message
     */
    public void showLoading(String message) {
        profileInfoMessage.setText(message);
        profileInfoMessage.setVisible(true);
        profileInfoError.setVisible(false);
        
        // Add a loading spinner animation if desired
        // This can be done by adding a ProgressIndicator to the FXML
    }
}

