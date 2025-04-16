// Path: src/main/java/controllers/LoginController.java
package com.esprit.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
 
import com.esprit.MainApp;
import com.esprit.models.User;
import com.esprit.models.enums.RoleEnum;
import com.esprit.services.AuthService;
import com.esprit.utils.SessionManager;
import com.esprit.utils.ValidationHelper;
import com.esprit.utils.ValidationUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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
                navigateToVerify();
            } else {
                errorLabel.setText("Failed to resend verification email. Please try again later.");
                errorLabel.setVisible(true);
            }
        }
    }
    
    private void navigateToVerify() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/verify.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) emailField.getScene().getWindow();
        
            // Use the overloaded method with larger dimensions for better visibility
            MainApp.setupStage(stage, root, "Verify Account - UNICLUBS", true, 700, 700);
            
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
                // For admin users, go to admin dashboard
                navigateToAdminDashboard();
            } else {
                // For all other users, navigate to home page
                navigateToHome();
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error navigating after login: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
    
    // Method to navigate to admin dashboard
   private void navigateToAdminDashboard() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/admin_dashboard.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) emailField.getScene().getWindow();
        
        // Use the utility method for consistent setup
        MainApp.setupStage(stage, root, "Admin Dashboard - UNICLUBS", false);
        
        stage.show();
    } catch (IOException e) {
        e.printStackTrace();
        showError("Error navigating to admin dashboard: " + e.getMessage());
    }
}
    
    // Method to navigate to home page
    private void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/home.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) emailField.getScene().getWindow();
            
            // Use the utility method for consistent setup
            MainApp.setupStage(stage, root, "Home - UNICLUBS", false);
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error navigating to home: " + e.getMessage());
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
        
        // Determine if this is a login-type screen or a main application screen
        boolean isLoginScreen = viewPath.contains("login") || viewPath.contains("register") || viewPath.contains("verify") || viewPath.contains("forgot");
        
        // Use the utility method for consistent setup
        MainApp.setupStage(stage, root, title, isLoginScreen);
        
        stage.show();
    } catch (IOException e) {
        e.printStackTrace();
        errorLabel.setText("Error loading view: " + e.getMessage());
        errorLabel.setVisible(true);
    }
}
    
   @FXML
private void navigateToRegister(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/register.fxml"));
    Parent root = loader.load();
    
    Stage stage = (Stage) emailField.getScene().getWindow();
    
    // Use even larger dimensions to ensure all content is visible
    MainApp.setupStage(stage, root, "Create Account - UNICLUBS", true, 800, 780);
    
    stage.show();
}

    
   @FXML
private void navigateToForgotPassword(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/forgot_password.fxml"));
    Parent root = loader.load();
    
    Stage stage = (Stage) emailField.getScene().getWindow();
    
    // Use the utility method for consistent setup
    MainApp.setupStage(stage, root, "Forgot Password", true);
    
    stage.show();
}
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

