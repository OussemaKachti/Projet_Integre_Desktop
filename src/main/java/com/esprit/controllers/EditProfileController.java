// Path: src/main/java/controllers/EditProfileController.java
package com.esprit.controllers;

import com.esprit.models.User;
import com.esprit.services.AuthService;
import com.esprit.utils.ProfanityFilter;
import com.esprit.utils.ProfanityLogManager;
import com.esprit.utils.ValidationHelper;
import com.esprit.utils.ValidationUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EditProfileController {

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
    private Label emailErrorLabel;
    
    @FXML
    private Label phoneErrorLabel;
    
    @FXML
    private Label firstNameErrorLabel;
    
    @FXML
    private Label lastNameErrorLabel;
    
    @FXML
    private Label passwordErrorLabel;
    
    @FXML
    private Label statusMessage;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private Button cancelButton;
    
    private User currentUser;
    private ProfileController parentController;
    private final AuthService authService = new AuthService();
    private ValidationHelper validator;
    
    @FXML
    private void initialize() {
        // Initialize the validation helper
        validator = new ValidationHelper()
            .addField(firstNameField, firstNameErrorLabel)
            .addField(lastNameField, lastNameErrorLabel)
            .addField(emailField, emailErrorLabel)
            .addField(phoneField, phoneErrorLabel)
            .addField(passwordField, passwordErrorLabel);
            
        // Hide status message initially
        statusMessage.setVisible(false);
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        populateFields();
    }
    
    public void setParentController(ProfileController controller) {
        this.parentController = controller;
    }
    
    private void populateFields() {
        if (currentUser != null) {
            firstNameField.setText(currentUser.getFirstName());
            lastNameField.setText(currentUser.getLastName());
            emailField.setText(currentUser.getEmail());
            phoneField.setText(currentUser.getPhone());
        }
    }
    
    @FXML
    private void handleSave() {
        // Reset validation state
        validator.reset();
        statusMessage.setVisible(false);
        
        // Get form data
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        
        // Validate first name
        boolean isFirstNameValid = validator.validateRequired(firstNameField, "First name is required");
        if (isFirstNameValid && firstName.length() < 2) {
            validator.showError(firstNameField, "First name must be at least 2 characters");
            isFirstNameValid = false;
        }
        
        // Check for profanity in first name
        if (isFirstNameValid && ProfanityFilter.containsProfanity(firstName)) {
            validator.showError(firstNameField, "First name contains inappropriate language");
            isFirstNameValid = false;
            
            // Log the profanity incident and increment warning count
            String severity = ProfanityLogManager.determineSeverity("First Name");
            ProfanityLogManager.logProfanityIncident(
                currentUser, 
                "First Name", 
                firstName,
                severity, 
                "Profile update rejected"
            );
        }
        
        // Validate last name
        boolean isLastNameValid = validator.validateRequired(lastNameField, "Last name is required");
        if (isLastNameValid && lastName.length() < 2) {
            validator.showError(lastNameField, "Last name must be at least 2 characters");
            isLastNameValid = false;
        }
        
        // Check for profanity in last name
        if (isLastNameValid && ProfanityFilter.containsProfanity(lastName)) {
            validator.showError(lastNameField, "Last name contains inappropriate language");
            isLastNameValid = false;
            
            // Log the profanity incident and increment warning count
            String severity = ProfanityLogManager.determineSeverity("Last Name");
            ProfanityLogManager.logProfanityIncident(
                currentUser, 
                "Last Name", 
                lastName,
                severity, 
                "Profile update rejected"
            );
        }
        
        // Validate email
        boolean isEmailValid = validator.validateRequired(emailField, "Email is required");
        if (isEmailValid && !ValidationUtils.isValidEmail(email)) {
            validator.showError(emailField, "Please enter a valid email address");
            isEmailValid = false;
        }
        
        // Validate phone (required)
        boolean isPhoneValid = validator.validateRequired(phoneField, "Phone number is required");
        if (isPhoneValid && !ValidationUtils.isValidPhone(phone)) {
            validator.showError(phoneField, "Please enter a valid phone number");
            isPhoneValid = false;
        }
        
        // Validate password (required)
        boolean isPasswordValid = validator.validateRequired(passwordField, "Current password is required");
        
        // If any validation failed, stop here
        if (!isFirstNameValid || !isLastNameValid || !isEmailValid || !isPhoneValid || !isPasswordValid) {
            return;
        }
        
        // Check if anything changed
        boolean hasChanges = !firstName.equals(currentUser.getFirstName()) ||
                            !lastName.equals(currentUser.getLastName()) ||
                            !email.equals(currentUser.getEmail()) ||
                            !phone.equals(currentUser.getPhone());
        
        if (!hasChanges) {
            // Nothing changed, just close
            close();
            return;
        }
        
        // Verify current password
        User authenticatedUser = authService.authenticate(currentUser.getEmail(), password);
        
        if (authenticatedUser == null) {
            // Password verification failed
            validator.showError(passwordField, "Current password is incorrect");
            return;
        }
        
        // Update user object
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        
        try {
            // Save changes to database
            boolean updateSuccess = authService.updateUserProfile(currentUser);
            
            if (updateSuccess) {
                // Update parent controller with new data
                if (parentController != null) {
                    parentController.updateUserData(currentUser);
                    parentController.showSuccess("Profile updated successfully");
                }
                
                // Close dialog
                close();
            } else {
                showError("Failed to update profile");
            }
        } catch (Exception e) {
            e.printStackTrace();
            
            // Check if it's a unique constraint violation
            if (ValidationUtils.isUniqueConstraintViolation(e, "email")) {
                validator.showError(emailField, "This email is already registered");
            } else if (ValidationUtils.isUniqueConstraintViolation(e, "tel")) {
                validator.showError(phoneField, "This phone number is already registered");
            } else {
                showError("Error: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleCancel() {
        close();
    }
    
  private void close() {
    try {
        // Get the current stage and close it
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    } catch (Exception e) {
        e.printStackTrace();
        // If there's an error closing normally, try alternative approach
        try {
            javafx.application.Platform.runLater(() -> {
                if (cancelButton != null && cancelButton.getScene() != null && 
                    cancelButton.getScene().getWindow() != null) {
                    cancelButton.getScene().getWindow().hide();
                } else if (saveButton != null && saveButton.getScene() != null && 
                           saveButton.getScene().getWindow() != null) {
                    saveButton.getScene().getWindow().hide();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
    
    private void showError(String message) {
        statusMessage.setText(message);
        statusMessage.getStyleClass().remove("alert-success");
        statusMessage.getStyleClass().add("alert-error");
        statusMessage.setVisible(true);
    }
    
    private void showSuccess(String message) {
        statusMessage.setText(message);
        statusMessage.getStyleClass().remove("alert-error");
        statusMessage.getStyleClass().add("alert-success");
        statusMessage.setVisible(true);
    }
}

