package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import services.AuthService;

import java.io.IOException;
import java.net.URL;

public class VerifyController {

    @FXML
    private TextField tokenField;
    
    @FXML
    private Label statusLabel;
    
    private final AuthService authService = new AuthService();
    
    @FXML
    private void initialize() {
        statusLabel.setVisible(false);
    }
    
    @FXML
    private void handleVerify(ActionEvent event) {
        String token = tokenField.getText().trim();
        
        if (token.isEmpty()) {
            showStatus("Please enter your verification token", true);
            return;
        }
        
        boolean verified = authService.verifyEmail(token);
        
        if (verified) {
            showStatus("Your account has been verified successfully!", false);
            
            // Show a success dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Verification Successful");
            alert.setHeaderText("Account Verified");
            alert.setContentText("Your account has been verified successfully! You can now login.");
            alert.showAndWait();
            
            // Navigate to login page
            navigateToLogin();
        } else {
            showStatus("Invalid or expired token. Please try again.", true);
        }
    }
    
    @FXML
    private void navigateToLogin() {
        try {
            URL resourceUrl = getClass().getResource("/views/login.fxml");
            if (resourceUrl == null) {
                showStatus("Login FXML file not found", true);
                return;
            }
            
            Parent root = FXMLLoader.load(resourceUrl);
            Stage stage = (Stage) tokenField.getScene().getWindow();
            stage.setScene(new Scene(root, 500, 400));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Error loading login page: " + e.getMessage(), true);
        }
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green"));
        statusLabel.setVisible(true);
    }
}