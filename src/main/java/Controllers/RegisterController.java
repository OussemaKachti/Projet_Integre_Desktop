// Path: src/main/java/controllers/RegisterController.java
package controllers;

import java.io.IOException;

import entities.User;
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
import test.MainApp;
import utils.ValidationHelper;
import utils.ValidationUtils;

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
    
    @FXML
    private Label firstNameErrorLabel;
    
    @FXML
    private Label lastNameErrorLabel;
    
    @FXML
    private Label emailErrorLabel;
    
    @FXML
    private Label phoneErrorLabel;
    
    @FXML
    private Label passwordErrorLabel;
    
    @FXML
    private Label confirmPasswordErrorLabel;
    
    private final AuthService authService = new AuthService();
    private ValidationHelper validator;
    
    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        
        // Initialize the validation helper
        validator = new ValidationHelper()
            .addField(firstNameField, firstNameErrorLabel)
            .addField(lastNameField, lastNameErrorLabel)
            .addField(emailField, emailErrorLabel)
            .addField(phoneField, phoneErrorLabel)
            .addField(passwordField, passwordErrorLabel)
            .addField(confirmPasswordField, confirmPasswordErrorLabel);
    }
    
    @FXML
    private void handleRegister(ActionEvent event) {
        // Reset validation state
        validator.reset();
        errorLabel.setVisible(false);
        
        // Get form data
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate first name
        boolean isFirstNameValid = validator.validateRequired(firstNameField, "First name is required");
        if (isFirstNameValid && firstName.length() < 2) {
            validator.showError(firstNameField, "First name must be at least 2 characters");
            isFirstNameValid = false;
        }
        
        // Validate last name
        boolean isLastNameValid = validator.validateRequired(lastNameField, "Last name is required");
        if (isLastNameValid && lastName.length() < 2) {
            validator.showError(lastNameField, "Last name must be at least 2 characters");
            isLastNameValid = false;
        }
        
        // Validate email
        boolean isEmailValid = validator.validateRequired(emailField, "Email is required");
        if (isEmailValid && !ValidationUtils.isValidEmail(email)) {
            validator.showError(emailField, "Please enter a valid email address");
            isEmailValid = false;
        }
        
        // Validate phone (now required)
        boolean isPhoneValid = validator.validateRequired(phoneField, "Phone number is required");
        if (isPhoneValid && !ValidationUtils.isValidPhone(phone)) {
            validator.showError(phoneField, "Please enter a valid Tunisian phone number");
            isPhoneValid = false;
        }
        
        // Validate password
        boolean isPasswordValid = validator.validateRequired(passwordField, "Password is required");
        if (isPasswordValid && !ValidationUtils.isValidPassword(password)) {
            validator.showError(passwordField, "Password must include uppercase, lowercase, numbers, and special characters");
            isPasswordValid = false;
        }
        
        // Validate confirm password
        boolean isConfirmPasswordValid = validator.validateRequired(confirmPasswordField, "Please confirm your password");
        if (isConfirmPasswordValid && !password.equals(confirmPassword)) {
            validator.showError(confirmPasswordField, "Passwords do not match");
            isConfirmPasswordValid = false;
        }
        
        // If any validation failed, stop here
        if (!isFirstNameValid || !isLastNameValid || !isEmailValid || 
            !isPhoneValid || !isPasswordValid || !isConfirmPasswordValid) {
            return;
        }
        
        // Create user object
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(phone); // Phone is now always set (no null check)
        
        // Register user
        try {
            User registeredUser = authService.registerUser(user);
            if (registeredUser == null) {
                errorLabel.setText("Registration failed. Please try again.");
                errorLabel.setVisible(true);
                return;
            }
            
            // Show success and navigate to verification page
            navigateToVerification(registeredUser);
        } catch (Exception e) {
            e.printStackTrace();
            
            // Check if it's a unique constraint violation and provide specific error message
            if (ValidationUtils.isUniqueConstraintViolation(e, "email")) {
                validator.showError(emailField, "This email is already registered");
            } else if (ValidationUtils.isUniqueConstraintViolation(e, "tel")) {
                validator.showError(phoneField, "This phone number is already registered");
            } else {
                // Generic error message for other exceptions
                errorLabel.setText("Error: " + e.getMessage());
                errorLabel.setVisible(true);
            }
        }
    }
    
    // Update the navigateToVerification method in RegisterController.java:

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/verify.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("Verify Your Account - UNICLUBS");
            
            // Create scene without explicit dimensions
            Scene scene = new Scene(root);
            stage.setScene(scene);
            
            // Keep login/verify screens at the smaller centered size
            MainApp.adjustStageSize(true);
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error loading verification page: " + e.getMessage());
            errorLabel.setVisible(true);
            // Fall back to login page
            navigateToLogin(null);
        }
    } catch (Exception e) {
        e.printStackTrace();
        errorLabel.setText("Error showing verification: " + e.getMessage());
        errorLabel.setVisible(true);
    }
}
    
    @FXML
    private void navigateToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("Login - UNICLUBS");
            
            // Create scene without explicit dimensions
            Scene scene = new Scene(root);
            stage.setScene(scene);
            
            // Keep login/register screens at the smaller size
            MainApp.adjustStageSize(true);
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error loading login page: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}