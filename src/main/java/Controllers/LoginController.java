package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import services.AuthService;
import entities.User;
import enums.RoleEnum;
import utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class LoginController {

    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;
    
    private final AuthService authService = new AuthService();
    
    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
    }
    
    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();
        
        if (email.isEmpty() || password.isEmpty()) {
            showError("Email and password are required");
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
                } else {
                    // Regular authentication failure
                    showError(authService.getLastAuthErrorMessage());
                    return;
                }
            }
            
            // Set current user in session
            SessionManager.getInstance().setCurrentUser(user);
            
            // Navigate to appropriate view based on role
            loadDashboard(user);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Authentication error: " + e.getMessage());
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
                showError("Failed to resend verification email. Please try again later.");
            }
        }
    }
    
    private void navigateToVerification() {
        try {
            URL resourceUrl = getClass().getResource("/views/verify.fxml");
            if (resourceUrl == null) {
                showError("Verification page not found");
                return;
            }
            
            Parent root = FXMLLoader.load(resourceUrl);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 500, 400));
            stage.setTitle("Verify Your Account");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading verification page: " + e.getMessage());
        }
    }
    
    private void loadDashboard(User user) {
        try {
            // For testing purposes only - show alert instead of loading FXML
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Login Successful");
            alert.setHeaderText("Login Success - Testing Mode");
            
            // Different message based on role
            String destinationInfo;
            if (user.getRole() == RoleEnum.ADMINISTRATEUR) {
                destinationInfo = "Would redirect to: Admin Dashboard";
            } else {
                destinationInfo = "Would redirect to: Homepage";
            }
            
            alert.setContentText("User successfully logged in as: " + user.getRole() + 
                            "\n" + destinationInfo);
            alert.showAndWait();
            
            // Don't attempt to load FXML files during testing
            return;
            
            /* Original code preserved but updated for your navigation flow
            String viewName;
            if (user.getRole() == RoleEnum.ADMINISTRATEUR) {
                viewName = "/views/admin_dashboard.fxml";
            } else {
                // NON_MEMBRE, MEMBRE, and PRESIDENT_CLUB all go to homepage
                viewName = "/views/homepage.fxml";
            }
            
            URL resourceUrl = getClass().getResource(viewName);
            if (resourceUrl == null) {
                showError("FXML file not found: " + viewName);
                return;
            }
            
            Parent root = FXMLLoader.load(resourceUrl);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Dashboard - " + user.getFirstName() + " " + user.getLastName());
            stage.show();
            */
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error in testing mode: " + e.getMessage());
        }
    }
    
    @FXML
    private void navigateToRegister(ActionEvent event) {
        try {
            URL resourceUrl = getClass().getResource("/views/register.fxml");
            if (resourceUrl == null) {
                showError("Register FXML file not found");
                return;
            }
            
            Parent root = FXMLLoader.load(resourceUrl);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("Register");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading registration page: " + e.getMessage());
        }
    }
    
    @FXML
    private void forgotPassword(ActionEvent event) {
        try {
            URL resourceUrl = getClass().getResource("/views/forgot_password.fxml");
            if (resourceUrl == null) {
                showError("Forgot password FXML file not found");
                return;
            }
            
            Parent root = FXMLLoader.load(resourceUrl);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 500, 300));
            stage.setTitle("Forgot Password");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading password reset page: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}