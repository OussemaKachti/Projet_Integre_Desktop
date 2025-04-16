package com.esprit.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.esprit.MainApp;
import com.esprit.services.AuthService;
import com.esprit.utils.ValidationHelper;
import com.esprit.utils.ValidationUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ForgotPasswordController implements Initializable {
    @FXML
    private Label statusLabel;
    
    @FXML
    private VBox stepOnePane;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private Label emailErrorLabel;
    
    @FXML
    private Button requestResetButton;
    
    private AuthService authService;
    private ValidationHelper validator;
    private String currentEmail;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = new AuthService();
        validator = new ValidationHelper();
        
        // Initialize the validation helper
        validator.addField(emailField, emailErrorLabel);
                
        statusLabel.setVisible(false);
    }
    
    /**
     * Set the email field with a specific email address
     * @param email The email address to set
     */
    public void setEmailField(String email) {
        if (emailField != null && email != null && !email.isEmpty()) {
            emailField.setText(email);
        }
    }
    
    @FXML
    private void handleRequestReset(ActionEvent event) {
        // Reset validation state
        validator.reset();
        statusLabel.setVisible(false);
        
        String email = emailField.getText().trim();
        
        // Validate email
        boolean isEmailValid = validator.validateRequired(emailField, "Email is required");
        if (isEmailValid && !ValidationUtils.isValidEmail(email)) {
            validator.showError(emailField, "Please enter a valid email address");
            isEmailValid = false;
        }
        
        if (!isEmailValid) {
            return;
        }
        
        // Disable the button while processing to prevent multiple requests
        requestResetButton.setDisable(true);
        requestResetButton.setText("Sending...");
        
        // Save the email for the next step
        currentEmail = email;
        
        // We'll process the reset in a background thread to keep UI responsive
        Thread resetThread = new Thread(() -> {
            // Request password reset
            String resetCode = authService.generatePasswordResetCode(email);
            
            // Update UI on the JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                // Re-enable the button
                requestResetButton.setDisable(false);
                requestResetButton.setText("Send Reset Code");
                
                if (resetCode != null) {
                    // Show success and navigate to verification page
                    showStatus("Reset code sent to your email address", false);
                    
                    // Navigate to verify code page after a short delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            javafx.application.Platform.runLater(() -> navigateToVerifyCode(email));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    // Show error
                    showStatus("We couldn't find an account with that email address", true);
                }
            });
        });
        
        // Start the thread
        resetThread.setDaemon(true);
        resetThread.start();
    }
    
    private void navigateToVerifyCode(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/verify_reset_code.fxml"));
            Parent root = loader.load();
            
            // Get controller and set the email
            VerifyResetCodeController controller = loader.getController();
            controller.setUserEmail(email);
            
            // Create a new stage instead of reusing the current one
            Stage newStage = new Stage();
            
            // Set up the new stage
            MainApp.setupStage(newStage, root, "Verify Reset Code - UNICLUBS", true, 700, 700);
            
            // Get the current stage to hide it
            Stage currentStage = (Stage) emailField.getScene().getWindow();
            currentStage.hide();
            
            // Show the new stage and ensure it's centered
            newStage.show();
            MainApp.ensureCentered(newStage);
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Error navigating to verification page: " + e.getMessage(), true);
        }
    }
    
    @FXML
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
            Parent root = loader.load();
            
            // Create a new stage instead of reusing the current one
            Stage newStage = new Stage();
            
            // Set up the new stage
            MainApp.setupStage(newStage, root, "Login - UNICLUBS", true, 700, 700);
            
            // Get the current stage to hide it
            Stage currentStage = (Stage) emailField.getScene().getWindow();
            currentStage.hide();
            
            // Show the new stage and ensure it's centered
            newStage.show();
            MainApp.ensureCentered(newStage);
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Error loading login page: " + e.getMessage(), true);
        }
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        
        if (isError) {
            statusLabel.getStyleClass().remove("alert-success");
            statusLabel.getStyleClass().add("alert-error");
        } else {
            statusLabel.getStyleClass().remove("alert-error");
            statusLabel.getStyleClass().add("alert-success");
        }
        
        statusLabel.setVisible(true);
    }
} 

