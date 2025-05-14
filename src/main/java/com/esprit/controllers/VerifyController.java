package com.esprit.controllers;

import java.io.IOException;

import com.esprit.MainApp;
import com.esprit.services.AuthService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

public class VerifyController {

    @FXML
    private TextField tokenField;

    @FXML
    private Label statusLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Button verifyButton;

    @FXML
    private Button resendButton;

    @FXML
    private Label timerLabel;

    private String userEmail;
    private final AuthService authService = new AuthService();
    private Timeline resendTimer;
    private int secondsRemaining = 60; // 1 minute cooldown for resend (reduced from 2 minutes)

    @FXML
    private void initialize() {
        statusLabel.setVisible(false);
        setupResendTimer();

        // Initially disable resend button and show timer
        resendButton.setDisable(true);
        timerLabel.setVisible(true);
        updateTimerLabel();

        // Start the timer immediately
        resendTimer.play();
    }

    /**
     * Set the user email for verification
     *
     * @param email Email address to verify
     */
    public void setUserEmail(String email) {
        this.userEmail = email;
        if (emailLabel != null) {
            emailLabel.setText(email);
        }
    }

    /**
     * Setup the countdown timer for resend button
     */
    private void setupResendTimer() {
        resendTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    secondsRemaining--;
                    updateTimerLabel();
                    if (secondsRemaining <= 0) {
                        resendButton.setDisable(false);
                        timerLabel.setVisible(false);
                        resendTimer.stop();
                    }
                })
        );
        resendTimer.setCycleCount(secondsRemaining);
    }

    /**
     * Update the timer label with current remaining time
     */
    private void updateTimerLabel() {
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        timerLabel.setText(String.format("Resend available in %02d:%02d", minutes, seconds));
    }

    /**
     * Handle verification button click
     */
    @FXML
    private void handleVerify(ActionEvent event) {
        String token = tokenField.getText().trim();

        if (token.isEmpty()) {
            showStatus("Please enter your verification code", true);
            return;
        }

        if (userEmail == null || userEmail.isEmpty()) {
            showStatus("Email address not provided", true);
            return;
        }

        // Check verification status first
        int status = authService.checkAccountVerificationStatus(userEmail);
        if (status == 1) {
            showSuccessAndRedirect("Your account is already verified!");
            return;
        } else if (status == 2) {
            showStatus("Too many failed attempts. Please contact support.", true);
            return;
        } else if (status == 3) {
            showStatus("Verification code has expired. Please request a new one.", true);
            return;
        }

        boolean verified = authService.verifyEmail(token, userEmail);

        if (verified) {
            showSuccessAndRedirect("Your account has been verified successfully!");
        } else {
            showStatus("Invalid verification code. Please try again.", true);
        }
    }

    /**
     * Handle resend verification code button click
     */
    @FXML
    private void handleResendCode(ActionEvent event) {
        if (userEmail == null || userEmail.isEmpty()) {
            showStatus("Email address not provided", true);
            return;
        }

        boolean sent = authService.resendVerificationCode(userEmail);

        if (sent) {
            showStatus("A new verification code has been sent to your email", false);

            // Reset and restart the timer
            secondsRemaining = 60; // Reset timer to 1 minute
            resendButton.setDisable(true);
            timerLabel.setVisible(true);
            updateTimerLabel();

            // Stop any existing timer and create a new one
            if (resendTimer != null) {
                resendTimer.stop();
            }
            setupResendTimer();
            resendTimer.play();
        } else {
            showStatus("Failed to send verification code. Please try again later.", true);
        }
    }

    /**
     * Show success message and redirect to login
     */
    private void showSuccessAndRedirect(String message) {
        showStatus(message, false);

        // Show a success dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Verification Successful");
        alert.setHeaderText("Account Verified");
        alert.setContentText("Your account has been verified successfully! You can now login.");
        alert.showAndWait();

        // Navigate to login page
        navigateToLogin();
    }

    /**
     * Show status message
     */
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().clear();
        if (isError) {
            statusLabel.getStyleClass().add("alert-error");
        } else {
            statusLabel.getStyleClass().add("alert-success");
        }
        statusLabel.setVisible(true);
    }

    /**
     * Navigate to login page
     */
    @FXML
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) tokenField.getScene().getWindow();

            // Use the utility method for consistent setup
            MainApp.setupStage(stage, root, "Login - UNICLUBS", true);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Error loading login page: " + e.getMessage(), true);
        }
    }
}
