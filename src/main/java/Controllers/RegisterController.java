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
import javafx.stage.Stage;
import services.AuthService;
import entities.User;
import utils.ValidationUtils;

import java.io.IOException;
import java.net.URL;

public class RegisterController {

    @FXML
    private TextField firstNameField;
    
    @FXML
    private TextField lastNameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label errorLabel;
    
    private final AuthService authService = new AuthService();
    
    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
    }
    
    @FXML
    private void handleRegister(ActionEvent event) {
        // Clear previous errors
        errorLabel.setVisible(false);
        
        // Get form data
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate input
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("First name, last name, email, and password are required");
            return;
        }
        
        // Validate email format
        if (!ValidationUtils.isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }
        
        // Validate password complexity
        if (!ValidationUtils.isValidPassword(password)) {
            showError("Password must include uppercase, lowercase, numbers, and special characters");
            return;
        }
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        // Create user object
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(phone.isEmpty() ? null : phone);
        
        // Register user
        try {
            User registeredUser = authService.registerUser(user);
            if (registeredUser == null) {
                showError("Registration failed. Please try again.");
                return;
            }
            
            // Show success and navigate to verification page
            navigateToVerification(registeredUser);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error: " + e.getMessage());
        }
    }
    
    private void navigateToVerification(User user) {
        try {
            // Show an alert with the verification token
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registration Successful");
            alert.setHeaderText("Account Created Successfully");
            alert.setContentText("A verification email has been sent to " + user.getEmail() + 
                ".\n\nYour verification token is: " + user.getConfirmationToken() + 
                "\n\nPlease use this token to verify your account.");
            alert.showAndWait();
            
            // Navigate to verification page
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
                // Fall back to login page
                navigateToLogin(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error showing verification: " + e.getMessage());
        }
    }
    
    @FXML
    private void navigateToLogin(ActionEvent event) {
        try {
            URL resourceUrl = getClass().getResource("/views/login.fxml");
            if (resourceUrl == null) {
                showError("Login FXML file not found");
                return;
            }
            
            Parent root = FXMLLoader.load(resourceUrl);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 500, 400));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading login page: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}