package controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import services.AuthService;
import test.MainApp;
import utils.ValidationHelper;

public class VerifyResetCodeController implements Initializable {

    @FXML
    private TextField codeField;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label emailLabel;
    
    @FXML
    private Button verifyButton;
    
    @FXML
    private Button resendButton;
    
    private String userEmail;
    private final AuthService authService = new AuthService();
    private ValidationHelper validator;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusLabel.setVisible(false);
        validator = new ValidationHelper();
    }
    
    /**
     * Set the user email for verification
     * @param email Email address to verify
     */
    public void setUserEmail(String email) {
        this.userEmail = email;
        if (emailLabel != null) {
            emailLabel.setText(email);
        }
    }
    
    @FXML
    private void handleVerifyCode(ActionEvent event) {
        validator.reset();
        statusLabel.setVisible(false);
        
        String code = codeField.getText().trim();
        
        if (code.isEmpty()) {
            showStatus("Please enter your reset code", true);
            return;
        }
        
        if (userEmail == null || userEmail.isEmpty()) {
            showStatus("Email address not provided", true);
            return;
        }
        
        // Check if the reset code is valid
        // This only checks if the token exists and is valid, doesn't reset the password yet
        boolean isValid = authService.verifyResetCode(code, userEmail);
        
        if (isValid) {
            // Navigate to the create new password screen
            navigateToCreatePassword(code, userEmail);
        } else {
            showStatus("Invalid or expired reset code. Please try again.", true);
        }
    }
    
    @FXML
    private void handleResendCode(ActionEvent event) {
        if (userEmail == null || userEmail.isEmpty()) {
            showStatus("Email address not provided", true);
            return;
        }
        
        // Disable button while processing
        resendButton.setDisable(true);
        
        // Process in background thread
        Thread resendThread = new Thread(() -> {
            String resetCode = authService.generatePasswordResetCode(userEmail);
            
            // Update UI on the JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                // Re-enable the button
                resendButton.setDisable(false);
                
                if (resetCode != null) {
                    showStatus("A new reset code has been sent to your email", false);
                } else {
                    showStatus("Failed to send reset code. Please try again later.", true);
                }
            });
        });
        
        resendThread.setDaemon(true);
        resendThread.start();
    }
    
    private void navigateToCreatePassword(String code, String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/create_new_password.fxml"));
            Parent root = loader.load();
            
            // Get controller and set the code and email
            CreateNewPasswordController controller = loader.getController();
            controller.setResetInfo(code, email);
            
            Stage stage = (Stage) codeField.getScene().getWindow();
            
            // Use consistent naming with the RegisterController
            MainApp.setupStage(stage, root, "Create New Password - UNICLUBS", true, 700, 700);
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Error navigating to password creation page: " + e.getMessage(), true);
        }
    }
    
    @FXML
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) codeField.getScene().getWindow();
            
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