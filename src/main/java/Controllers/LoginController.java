// Path: src/main/java/controllers/LoginController.java
package controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
 
import entities.User;
import enums.RoleEnum;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import services.AuthService;
import test.MainApp;
import utils.SessionManager;
import utils.ValidationHelper;
import utils.ValidationUtils;

public class LoginController {

    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private Label emailErrorLabel;
    
    @FXML
    private Label passwordErrorLabel;
    
    private final AuthService authService = new AuthService();
    private ValidationHelper validator;
    
    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        
        // Initialize the validation helper
        validator = new ValidationHelper()
            .addField(emailField, emailErrorLabel)
            .addField(passwordField, passwordErrorLabel);
    }
    
    @FXML
    private void handleLogin(ActionEvent event) {
        // Reset validation state
        validator.reset();
        errorLabel.setVisible(false);
        
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        // Validate email
        boolean isEmailValid = validator.validateRequired(emailField, "Email is required");
        if (isEmailValid && !ValidationUtils.isValidEmail(email)) {
            validator.showError(emailField, "Please enter a valid email address");
            isEmailValid = false;
        }
        
        // Validate password
        boolean isPasswordValid = validator.validateRequired(passwordField, "Password is required");
        
        // If any validation failed, stop here
        if (!isEmailValid || !isPasswordValid) {
            return;
        }
        
        try {
            User user = authService.authenticate(email, password);
            
            if (user == null) {
                // Check the error code to provide appropriate message
                int errorCode = authService.getLastAuthErrorCode();
                
                if (errorCode == AuthService.AUTH_NOT_VERIFIED) {
                    // Account not verified - show special error and option to resend verification
                    showNotVerifiedError(email);
                    return;
                } else if (errorCode == AuthService.AUTH_INVALID_CREDENTIALS) {
                    // For security reasons, don't specify whether email or password is wrong
                    errorLabel.setText("Invalid email or password");
                    errorLabel.setVisible(true);
                    return;
                } else {
                    // Regular authentication failure
                    errorLabel.setText(authService.getLastAuthErrorMessage());
                    errorLabel.setVisible(true);
                    return;
                }
            }
            
            // Set current user in session
            SessionManager.getInstance().setCurrentUser(user);
            
            // Navigate to appropriate view based on role
            loadDashboard(user);
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Authentication error: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
    
    private void showNotVerifiedError(String email) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Account Not Verified");
        alert.setHeaderText("Email Verification Required");
        alert.setContentText("Your account has not been verified. Please check your email for verification instructions.");
        
        // Add a button to resend verification email
        ButtonType resendButton = new ButtonType("Resend Verification Email");
        ButtonType cancelButton = ButtonType.CANCEL;
        
        alert.getButtonTypes().setAll(resendButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == resendButton) {
            // User chose to resend verification email
            boolean sent = authService.resendVerificationEmail(email);
            if (sent) {
                Alert successAlert = new Alert(AlertType.INFORMATION);
                successAlert.setTitle("Verification Email Sent");
                successAlert.setHeaderText("Check Your Email");
                successAlert.setContentText("A new verification email has been sent to " + email + 
                                            "\n\nYou will now be redirected to the verification page.");
                successAlert.showAndWait();
                
                // Navigate to verification page
                navigateToVerification();
            } else {
                errorLabel.setText("Failed to resend verification email. Please try again later.");
                errorLabel.setVisible(true);
            }
        }
    }
    
    private void navigateToVerification() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/verify.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("Verify Account - UNICLUBS");
            
            // Create scene without explicit dimensions
            Scene scene = new Scene(root);
            stage.setScene(scene);
            
            // Keep login/verify screens at the smaller size
            MainApp.adjustStageSize(true);
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error navigating to verification: " + e.getMessage());
        }
    }
    
    private void loadDashboard(User user) {
        try {
            // Navigate based on user role
            if (user.getRole() == RoleEnum.ADMINISTRATEUR) {
                // For admin users, go to admin dashboard (keeping alert for now)
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Login Successful");
                alert.setHeaderText("Admin Login Success");
                alert.setContentText("User successfully logged in as: " + user.getRole() + 
                                "\nWould redirect to: Admin Dashboard");
                alert.showAndWait();
                
                // When admin dashboard is ready, use this code:
                // navigateToView("/views/admin_dashboard.fxml", "Admin Dashboard");
            } else {
                // For all other users, navigate to profile page
                navigateToProfile();
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error navigating after login: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
    
    // Navigate to profile page
  // Update the navigateToProfile method in LoginController.java to properly maximize:

private void navigateToProfile() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/profile.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setTitle("My Profile - UNICLUBS");
        
        // Create scene without explicit dimensions
        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        // Set to main application screens (maximized)
        MainApp.adjustStageSize(false);
        
        stage.show();
    } catch (IOException e) {
        e.printStackTrace();
        showError("Error navigating to profile: " + e.getMessage());
    }
}
    
    // Helper method for navigation
    private void navigateToView(String viewPath, String title) {
        try {
            URL resourceUrl = getClass().getResource(viewPath);
            if (resourceUrl == null) {
                errorLabel.setText("View not found: " + viewPath);
                errorLabel.setVisible(true);
                return;
            }
            
            Parent root = FXMLLoader.load(resourceUrl);
            Stage stage = (Stage) emailField.getScene().getWindow();
            
            // Create scene without explicit dimensions
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            
            // Let MainApp handle sizing
            MainApp.adjustStageSize(false);
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error loading view: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
    
    @FXML
    private void navigateToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/register.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("Register - UNICLUBS");
            
            // Create scene without explicit dimensions
            Scene scene = new Scene(root);
            stage.setScene(scene);
            
            // Keep login/register screens at the smaller size
            MainApp.adjustStageSize(true);
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error navigating to registration: " + e.getMessage());
        }
    }
    
    @FXML
    private void forgotPassword(ActionEvent event) {
        try {
            URL resourceUrl = getClass().getResource("/views/forgot_password.fxml");
            if (resourceUrl == null) {
                errorLabel.setText("Forgot password FXML file not found");
                errorLabel.setVisible(true);
                return;
            }
            
            Parent root = FXMLLoader.load(resourceUrl);
            Stage stage = (Stage) emailField.getScene().getWindow();
            
            // Create scene without explicit dimensions
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Forgot Password");
            
            // Use login screen sizing
            MainApp.adjustStageSize(true);
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error loading password reset page: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}