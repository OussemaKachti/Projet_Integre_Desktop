package com.esprit.controllers;

import com.esprit.models.User;
import com.esprit.services.AuthService;
import com.esprit.utils.AiContentValidator;
import com.esprit.utils.ProfanityFilter;
import com.esprit.utils.ProfanityLogManager;
import com.esprit.utils.ValidationHelper;
import com.esprit.utils.ValidationUtils;

import javafx.application.Platform;
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
    
    // Add validation status labels
    @FXML
    private Label firstNameValidationStatus;
    
    @FXML
    private Label lastNameValidationStatus;
    
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
        
        // Initialize AI validation status labels
        if (firstNameValidationStatus != null) {
            firstNameValidationStatus.setVisible(false);
        }
        
        if (lastNameValidationStatus != null) {
            lastNameValidationStatus.setVisible(false);
        }
        
        // Setup AI validations
        setupAiValidations();
        
        // Add a small delay to ensure the save button is enabled after all initializations
        javafx.application.Platform.runLater(() -> {
            // Ensure save button is enabled by default - this must run after all other initialization
            saveButton.setDisable(false);
        });
    }
    
    private void setupAiValidations() {
        // Add AI validation to name fields
        firstNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && !newValue.equals(oldValue) && firstNameValidationStatus != null) {
                firstNameValidationStatus.setText("Validating...");
                firstNameValidationStatus.setVisible(true);
                
                AiContentValidator.validateNameAsync(newValue, (isValid, message) -> {
                    Platform.runLater(() -> {
                        if (isValid) {
                            firstNameErrorLabel.setVisible(false);
                            firstNameValidationStatus.setText("✓");
                            firstNameValidationStatus.setStyle("-fx-text-fill: green;");
                        } else {
                            firstNameErrorLabel.setText(message);
                            firstNameErrorLabel.setVisible(true);
                            firstNameValidationStatus.setText("✗");
                            firstNameValidationStatus.setStyle("-fx-text-fill: red;");
                            // Log the detected inappropriate content
                            System.out.println("Edit profile - first name validation failed: " + message + " for text: " + newValue);
                        }
                        firstNameValidationStatus.setVisible(true);
                        // Always make sure the save button is enabled regardless of validation
                        saveButton.setDisable(false);
                    });
                });
            } else if (firstNameValidationStatus != null) {
                firstNameValidationStatus.setVisible(false);
                firstNameErrorLabel.setVisible(false);
            }
            // Keep save button enabled no matter what
            saveButton.setDisable(false);
        });
        
        lastNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && !newValue.equals(oldValue) && lastNameValidationStatus != null) {
                lastNameValidationStatus.setText("Validating...");
                lastNameValidationStatus.setVisible(true);
                
                AiContentValidator.validateNameAsync(newValue, (isValid, message) -> {
                    Platform.runLater(() -> {
                        if (isValid) {
                            lastNameErrorLabel.setVisible(false);
                            lastNameValidationStatus.setText("✓");
                            lastNameValidationStatus.setStyle("-fx-text-fill: green;");
                        } else {
                            lastNameErrorLabel.setText(message);
                            lastNameErrorLabel.setVisible(true);
                            lastNameValidationStatus.setText("✗");
                            lastNameValidationStatus.setStyle("-fx-text-fill: red;");
                            // Log the detected inappropriate content
                            System.out.println("Edit profile - last name validation failed: " + message + " for text: " + newValue);
                        }
                        lastNameValidationStatus.setVisible(true);
                        // Always make sure the save button is enabled regardless of validation
                        saveButton.setDisable(false);
                    });
                });
            } else if (lastNameValidationStatus != null) {
                lastNameValidationStatus.setVisible(false);
                lastNameErrorLabel.setVisible(false);
            }
            // Keep save button enabled no matter what
            saveButton.setDisable(false);
        });
        
        // Add listeners to other fields that also ensure the save button stays enabled
        emailField.textProperty().addListener((obs, old, newValue) -> saveButton.setDisable(false));
        phoneField.textProperty().addListener((obs, old, newValue) -> saveButton.setDisable(false));
        passwordField.textProperty().addListener((obs, old, newValue) -> saveButton.setDisable(false));
    }
    
    // Method to update save button state - always enabled
    private void updateSaveButtonState() {
        // Always enable the save button regardless of validation
        saveButton.setDisable(false);
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
            
            // Explicitly enable the save button after populating fields
            saveButton.setDisable(false);
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
        
        // Password is required - check this first
        if (password.isEmpty()) {
            validator.showError(passwordField, "Current password is required");
            return; // Stop here if no password
        }
        
        boolean contentViolationDetected = false;
        
        // Add warning flags to prevent double counting
        boolean firstNameWarningIssued = false;
        boolean lastNameWarningIssued = false;
        
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
            
            // Add warning and send email notification
            authService.addContentWarning(currentUser, "profile first name");
            contentViolationDetected = true;
            firstNameWarningIssued = true; // Set flag to prevent duplicate warning
        }
        
        // Add AI validation for first name
        if (isFirstNameValid && !AiContentValidator.isAppropriateContent(firstName)) {
            validator.showError(firstNameField, "First name contains inappropriate content");
            isFirstNameValid = false;
            
            // Log the AI validation incident
            String severity = "High";
            ProfanityLogManager.logProfanityIncident(
                currentUser, 
                "First Name (AI detection)", 
                firstName,
                severity, 
                "Profile update rejected by AI validation"
            );
            
            // Add warning and send email notification ONLY if not already issued
            if (!firstNameWarningIssued) {
                authService.addContentWarning(currentUser, "profile first name");
                contentViolationDetected = true;
            }
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
            
            // Log the profanity incident
            String severity = ProfanityLogManager.determineSeverity("Last Name");
            ProfanityLogManager.logProfanityIncident(
                currentUser, 
                "Last Name", 
                lastName,
                severity, 
                "Profile update rejected"
            );
            
            // Add warning and send email notification
            authService.addContentWarning(currentUser, "profile last name");
            contentViolationDetected = true;
            lastNameWarningIssued = true; // Set flag to prevent duplicate warning
        }
        
        // Add AI validation for last name
        if (isLastNameValid && !AiContentValidator.isAppropriateContent(lastName)) {
            validator.showError(lastNameField, "Last name contains inappropriate content");
            isLastNameValid = false;
            
            // Log the AI validation incident
            String severity = "High";
            ProfanityLogManager.logProfanityIncident(
                currentUser, 
                "Last Name (AI detection)", 
                lastName,
                severity, 
                "Profile update rejected by AI validation"
            );
            
            // Add warning and send email notification ONLY if not already issued
            if (!lastNameWarningIssued) {
                authService.addContentWarning(currentUser, "profile last name");
                contentViolationDetected = true;
            }
        }
        
        // Display special error message if content violation was detected
        if (contentViolationDetected) {
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
                
                // Close the edit dialog
                close();
                
                // Logout the user via the parent controller
                if (parentController != null) {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            // Clear session
                            com.esprit.utils.SessionManager.getInstance().clearSession();
                            // Navigate to login
                            parentController.handleLogout(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                return;
            }
            
            showError("Your profile contains inappropriate content. A warning has been recorded on your account.");
            return;
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
        
        // If any validation failed, stop here
        if (!isFirstNameValid || !isLastNameValid || !isEmailValid || !isPhoneValid) {
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
        
        // Verify current password - do this BEFORE updating the user object
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