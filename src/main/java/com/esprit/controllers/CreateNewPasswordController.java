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
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CreateNewPasswordController implements Initializable {
    @FXML
    private Label statusLabel;
    
    @FXML
    private PasswordField newPasswordField;
    
    @FXML
    private Label passwordErrorLabel;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label confirmPasswordErrorLabel;
    
    @FXML
    private VBox passwordRequirementsBox;
    
    @FXML
    private Button resetPasswordButton;
    
    private AuthService authService;
    private ValidationHelper validator;
    private String resetCode;
    private String userEmail;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = new AuthService();
        validator = new ValidationHelper();
        
        // Initialize the validation helper
        validator.addField(newPasswordField, passwordErrorLabel)
                .addField(confirmPasswordField, confirmPasswordErrorLabel);
        
        // Show password requirements when password field is focused
        newPasswordField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            passwordRequirementsBox.setVisible(newValue);
        });
        
        statusLabel.setVisible(false);
    }
    
    /**
     * Set the reset code and email information
     */
    public void setResetInfo(String code, String email) {
        this.resetCode = code;
        this.userEmail = email;
    }
    
    @FXML
    private void handleResetPassword(ActionEvent event) {
        // Reset validation state
        validator.reset();
        statusLabel.setVisible(false);
        
        String password = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Check if we have the reset code
        if (resetCode == null || resetCode.isEmpty() || userEmail == null || userEmail.isEmpty()) {
            showStatus("Missing reset information, please try again", true);
            return;
        }
        
        // Validate password
        boolean isPasswordValid = validator.validateRequired(newPasswordField, "New password is required");
        if (isPasswordValid && !ValidationUtils.isValidPassword(password)) {
            validator.showError(newPasswordField, "Password does not meet requirements");
            isPasswordValid = false;
        }
        
        // Validate confirm password
        boolean isConfirmValid = validator.validateRequired(confirmPasswordField, "Please confirm your password");
        if (isConfirmValid && !password.equals(confirmPassword)) {
            validator.showError(confirmPasswordField, "Passwords do not match");
            isConfirmValid = false;
        }
        
        if (!isPasswordValid || !isConfirmValid) {
            return;
        }
        
        // Verify the reset code is valid first
        if (!authService.verifyResetCode(resetCode, userEmail)) {
            showStatus("Invalid or expired reset code. Please request a new one.", true);
            return;
        }
        
        // Reset password
        boolean success = authService.resetPassword(resetCode, password);
        if (success) {
            // Show success message
            showStatus("Your password has been reset successfully", false);
            
            // Navigate to login after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> navigateToLogin());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            // The only way resetPassword would fail at this point (after verifyResetCode succeeded)
            // is if the new password is the same as the current password
            showStatus("Please choose a password different from your current one", true);
        }
    }
    
    @FXML
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) newPasswordField.getScene().getWindow();
            
            // Use the overloaded method with appropriate dimensions for login
            MainApp.setupStage(stage, root, "Login - UNICLUBS", true, 700, 700);
            
            stage.show();
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

