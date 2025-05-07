package com.esprit.controllers;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.esprit.MainApp;
import com.esprit.models.User;
import com.esprit.services.AuthService;
import com.esprit.utils.AiContentValidator;
import com.esprit.utils.ValidationHelper;
import com.esprit.utils.ValidationUtils;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    private static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());

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
    
    // Add new validation status labels for AI feedback
    @FXML
    private Label firstNameValidationStatus;
    
    @FXML
    private Label lastNameValidationStatus;
    
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
            
        // Initialize AI validation status labels
        if (firstNameValidationStatus != null) {
            firstNameValidationStatus.setVisible(false);
        }
        
        if (lastNameValidationStatus != null) {
            lastNameValidationStatus.setVisible(false);
        }
        
        // Setup AI-powered content validation
        setupValidations();
    }
    
    private void setupValidations() {
        // Add real-time AI validation to name fields with debouncing
        final long[] firstNameLastValidation = {0};
        final long[] lastNameLastValidation = {0};
        final long DEBOUNCE_DELAY = 500; // 500ms debounce

        firstNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && firstNameValidationStatus != null) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - firstNameLastValidation[0] < DEBOUNCE_DELAY) {
                    return; // Skip if too soon after last validation
                }
                firstNameLastValidation[0] = currentTime;

                firstNameValidationStatus.setText("Validating...");
                firstNameValidationStatus.setVisible(true);
                
                // Run validation in background thread
                CompletableFuture.runAsync(() -> {
                    AiContentValidator.validateNameAsync(newValue, (isValid, message) -> {
                        Platform.runLater(() -> {
                            if (isValid) {
                                firstNameValidationStatus.setText("✓");
                                firstNameValidationStatus.setStyle("-fx-text-fill: green;");
                                firstNameErrorLabel.setVisible(false);
                            } else {
                                firstNameValidationStatus.setText("✗");
                                firstNameValidationStatus.setStyle("-fx-text-fill: red;");
                                firstNameErrorLabel.setText(message);
                                firstNameErrorLabel.setVisible(true);
                                LOGGER.log(Level.INFO, "First name validation failed: {0} for text: {1}", 
                                    new Object[]{message, newValue});
                            }
                            firstNameValidationStatus.setVisible(true);
                        });
                    });
                });
            } else if (firstNameValidationStatus != null) {
                firstNameValidationStatus.setVisible(false);
                firstNameErrorLabel.setVisible(false);
            }
        });
        
        lastNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && lastNameValidationStatus != null) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastNameLastValidation[0] < DEBOUNCE_DELAY) {
                    return; // Skip if too soon after last validation
                }
                lastNameLastValidation[0] = currentTime;

                lastNameValidationStatus.setText("Validating...");
                lastNameValidationStatus.setVisible(true);
                
                // Run validation in background thread
                CompletableFuture.runAsync(() -> {
                    AiContentValidator.validateNameAsync(newValue, (isValid, message) -> {
                        Platform.runLater(() -> {
                            if (isValid) {
                                lastNameValidationStatus.setText("✓");
                                lastNameValidationStatus.setStyle("-fx-text-fill: green;");
                                lastNameErrorLabel.setVisible(false);
                            } else {
                                lastNameValidationStatus.setText("✗");
                                lastNameValidationStatus.setStyle("-fx-text-fill: red;");
                                lastNameErrorLabel.setText(message);
                                lastNameErrorLabel.setVisible(true);
                                LOGGER.log(Level.INFO, "Last name validation failed: {0} for text: {1}", 
                                    new Object[]{message, newValue});
                            }
                            lastNameValidationStatus.setVisible(true);
                        });
                    });
                });
            } else if (lastNameValidationStatus != null) {
                lastNameValidationStatus.setVisible(false);
                lastNameErrorLabel.setVisible(false);
            }
        });
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
        
        // Basic validation first
        boolean isFirstNameValid = validator.validateRequired(firstNameField, "First name is required");
        boolean isLastNameValid = validator.validateRequired(lastNameField, "Last name is required");
        boolean isEmailValid = validator.validateRequired(emailField, "Email is required");
        boolean isPhoneValid = validator.validateRequired(phoneField, "Phone number is required");
        boolean isPasswordValid = validator.validateRequired(passwordField, "Password is required");
        boolean isConfirmPasswordValid = validator.validateRequired(confirmPasswordField, "Please confirm your password");
        
        // If any basic validation failed, stop here
        if (!isFirstNameValid || !isLastNameValid || !isEmailValid || 
            !isPhoneValid || !isPasswordValid || !isConfirmPasswordValid) {
            return;
        }
        
        // Additional validation
        if (firstName.length() < 2) {
            validator.showError(firstNameField, "First name must be at least 2 characters");
            return;
        }
        
        if (lastName.length() < 2) {
            validator.showError(lastNameField, "Last name must be at least 2 characters");
            return;
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            validator.showError(emailField, "Please enter a valid email address");
            return;
        }
        
        if (!ValidationUtils.isValidPhone(phone)) {
            validator.showError(phoneField, "Please enter a valid Tunisian phone number");
            return;
        }
        
        if (!ValidationUtils.isValidPassword(password)) {
            validator.showError(passwordField, "Password must include uppercase, lowercase, numbers, and special characters");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            validator.showError(confirmPasswordField, "Passwords do not match");
            return;
        }
        
        // Disable the register button to prevent double submission
        Button registerButton = (Button) event.getSource();
        registerButton.setDisable(true);
        
        // Show loading state
        errorLabel.setText("Validating...");
        errorLabel.setVisible(true);
        
        // Run AI validation in background with timeout
        CompletableFuture.runAsync(() -> {
            try {
                // Set a timeout for AI validation
                CompletableFuture<Boolean> firstNameValidation = CompletableFuture.supplyAsync(() -> {
                    try {
                        return AiContentValidator.isAppropriateContent(firstName);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "First name AI validation failed: {0}", e.getMessage());
                        return true; // Allow registration on AI failure
                    }
                }).orTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                  .exceptionally(ex -> {
                      LOGGER.log(Level.WARNING, "First name AI validation timed out: {0}", ex.getMessage());
                      return true; // Allow registration on timeout
                  });

                CompletableFuture<Boolean> lastNameValidation = CompletableFuture.supplyAsync(() -> {
                    try {
                        return AiContentValidator.isAppropriateContent(lastName);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Last name AI validation failed: {0}", e.getMessage());
                        return true; // Allow registration on AI failure
                    }
                }).orTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                  .exceptionally(ex -> {
                      LOGGER.log(Level.WARNING, "Last name AI validation timed out: {0}", ex.getMessage());
                      return true; // Allow registration on timeout
                  });

                // Wait for both validations with timeout
                boolean isFirstNameAppropriate = firstNameValidation.get(10, java.util.concurrent.TimeUnit.SECONDS);
                boolean isLastNameAppropriate = lastNameValidation.get(10, java.util.concurrent.TimeUnit.SECONDS);

                if (!isFirstNameAppropriate) {
                    Platform.runLater(() -> {
                        validator.showError(firstNameField, "First name contains inappropriate content");
                        registerButton.setDisable(false);
                        errorLabel.setVisible(false);
                    });
                    return;
                }
                
                if (!isLastNameAppropriate) {
                    Platform.runLater(() -> {
                        validator.showError(lastNameField, "Last name contains inappropriate content");
                        registerButton.setDisable(false);
                        errorLabel.setVisible(false);
                    });
                    return;
                }
                
                // If we get here, all validations passed
                Platform.runLater(() -> {
                    // Create user object
                    User user = new User();
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setEmail(email);
                    user.setPassword(password);
                    user.setPhone(phone);
                    
                    // Register user
                    try {
                        User registeredUser = authService.registerUser(user);
                        if (registeredUser == null) {
                            errorLabel.setText("Registration failed. Please try again.");
                            errorLabel.setVisible(true);
                            registerButton.setDisable(false);
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
                        registerButton.setDisable(false);
                    }
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Registration validation failed: {0}", e.getMessage());
                Platform.runLater(() -> {
                    // On any validation error, proceed with registration
                    // Create user object
                    User user = new User();
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setEmail(email);
                    user.setPassword(password);
                    user.setPhone(phone);
                    
                    try {
                        User registeredUser = authService.registerUser(user);
                        if (registeredUser == null) {
                            errorLabel.setText("Registration failed. Please try again.");
                            errorLabel.setVisible(true);
                            registerButton.setDisable(false);
                            return;
                        }
                        
                        // Show success and navigate to verification page
                        navigateToVerification(registeredUser);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (ValidationUtils.isUniqueConstraintViolation(ex, "email")) {
                            validator.showError(emailField, "This email is already registered");
                        } else if (ValidationUtils.isUniqueConstraintViolation(ex, "tel")) {
                            validator.showError(phoneField, "This phone number is already registered");
                        } else {
                            errorLabel.setText("Error: " + ex.getMessage());
                            errorLabel.setVisible(true);
                        }
                        registerButton.setDisable(false);
                    }
                });
            }
        });
    }
    
    private void navigateToVerification(User user) {
        try {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/verify.fxml"));
                Parent root = loader.load();
                
                // Pass the email to verification controller
                VerifyController controller = loader.getController();
                controller.setUserEmail(user.getEmail());
                
                Stage stage = (Stage) emailField.getScene().getWindow();
                
                // Use the overloaded method with larger dimensions for better visibility
                MainApp.setupStage(stage, root, "Verify Your Account - UNICLUBS", true, 750, 700);
                
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Error loading verification page: " + e.getMessage());
                errorLabel.setVisible(true);
                // Fall back to login page if verify page can't be loaded
                navigateToLogin(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error showing verification: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }
    
    @FXML
    private void navigateToLogin(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) emailField.getScene().getWindow();
        
        // Use the overloaded method with appropriate dimensions for login
        MainApp.setupStage(stage, root, "Login - UNICLUBS", true, 700, 700);
        
        stage.show();
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}