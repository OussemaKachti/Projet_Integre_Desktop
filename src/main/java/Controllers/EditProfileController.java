// Path: src/main/java/controllers/EditProfileController.java
package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import services.AuthService;
import entities.User;
import utils.ValidationUtils;

public class EditProfileController {

    @FXML
    private TextField nameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private PasswordField currentPasswordField;
    
    @FXML
    private Label nameError;
    
    @FXML
    private Label emailError;
    
    @FXML
    private Label phoneError;
    
    @FXML
    private Label passwordError;
    
    @FXML
    private Label errorMessageLabel;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button updateButton;
    
    private User currentUser;
    private ProfileController parentController;
    private final AuthService authService = new AuthService();
    
    @FXML
    private void initialize() {
        // Setup field validation listeners
        setupValidationListeners();
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        
        // Populate fields with current user data
        nameField.setText(user.getFirstName() + " " + user.getLastName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
    }
    
    public void setParentController(ProfileController controller) {
        this.parentController = controller;
    }
    
    private void setupValidationListeners() {
        // Real-time validation for name field
        nameField.textProperty().addListener((obs, oldValue, newValue) -> {
            validateName(newValue);
        });
        
        // Real-time validation for email field
        emailField.textProperty().addListener((obs, oldValue, newValue) -> {
            validateEmail(newValue);
        });
        
        // Real-time validation for phone field
        phoneField.textProperty().addListener((obs, oldValue, newValue) -> {
            validatePhone(newValue);
        });
    }
    
    private boolean validateName(String name) {
        nameError.setVisible(false);
        
        if (name.trim().isEmpty()) {
            nameError.setText("Name cannot be empty");
            nameError.setVisible(true);
            return false;
        }
        
        String[] parts = name.trim().split("\\s+");
        if (parts.length < 2) {
            nameError.setText("Please provide both first and last name");
            nameError.setVisible(true);
            return false;
        }
        
        String firstName = parts[0];
        String lastName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        
        // Check for numbers or special characters
        if (!firstName.matches("^[a-zA-ZÀ-ÿ\\s'-]+") || !lastName.matches("^[a-zA-ZÀ-ÿ\\s'-]+")) {
            nameError.setText("Name can only contain letters, spaces, hyphens and apostrophes");
            nameError.setVisible(true);
            return false;
        }
        
        return true;
    }
    
    private boolean validateEmail(String email) {
        emailError.setVisible(false);
        
        if (email.trim().isEmpty()) {
            emailError.setText("Email cannot be empty");
            emailError.setVisible(true);
            return false;
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            emailError.setText("Please enter a valid email address");
            emailError.setVisible(true);
            return false;
        }
        
        return true;
    }
    
    private boolean validatePhone(String phone) {
        phoneError.setVisible(false);
        
        if (phone.trim().isEmpty()) {
            phoneError.setText("Phone number cannot be empty");
            phoneError.setVisible(true);
            return false;
        }
        
        if (!ValidationUtils.isValidPhone(phone)) {
            phoneError.setText("Please enter a valid Tunisian phone number");
            phoneError.setVisible(true);
            return false;
        }
        
        return true;
    }
    
    private boolean validatePassword(String password) {
        passwordError.setVisible(false);
        
        if (password.trim().isEmpty()) {
            passwordError.setText("Current password is required to confirm changes");
            passwordError.setVisible(true);
            return false;
        }
        
        return true;
    }
    
    @FXML
    private void handleUpdate(ActionEvent event) {
        // Clear error messages
        errorMessageLabel.setVisible(false);
        
        // Validate all fields
        boolean isNameValid = validateName(nameField.getText());
        boolean isEmailValid = validateEmail(emailField.getText());
        boolean isPhoneValid = validatePhone(phoneField.getText());
        boolean isPasswordValid = validatePassword(currentPasswordField.getText());
        
        // If any validation fails, stop
        if (!isNameValid || !isEmailValid || !isPhoneValid || !isPasswordValid) {
            return;
        }
        
        try {
            // Parse name into first and last name
            String[] nameParts = nameField.getText().trim().split("\\s+");
            String firstName = nameParts[0];
            String lastName = String.join(" ", java.util.Arrays.copyOfRange(nameParts, 1, nameParts.length));
            
            // Authenticate with current password
            User authenticatedUser = authService.authenticate(currentUser.getEmail(), currentPasswordField.getText());
            
            if (authenticatedUser == null) {
                passwordError.setText("Current password is incorrect");
                passwordError.setVisible(true);
                return;
            }
            
            // Check if email is already in use (if changed)
            if (!emailField.getText().equals(currentUser.getEmail())) {
                User existingUser = authService.findUserByEmail(emailField.getText());
                if (existingUser != null) {
                    emailError.setText("This email is already registered");
                    emailError.setVisible(true);
                    return;
                }
            }
            
            // Check if phone is already in use (if changed)
            if (!phoneField.getText().equals(currentUser.getPhone())) {
                User existingUser = authService.findUserByPhone(phoneField.getText());
                if (existingUser != null) {
                    phoneError.setText("This phone number is already registered");
                    phoneError.setVisible(true);
                    return;
                }
            }
            
            // Update user object
            currentUser.setFirstName(firstName);
            currentUser.setLastName(lastName);
            currentUser.setEmail(emailField.getText());
            currentUser.setPhone(phoneField.getText());
            
            // Save changes
            boolean updated = authService.updateUserProfile(currentUser);
            
            if (updated) {
                // Update parent controller
                parentController.updateUserData(currentUser);
                parentController.showSuccess("Profile updated successfully!");
                
                // Close the dialog
                closeDialog();
            } else {
                errorMessageLabel.setText("Failed to update profile. Please try again.");
                errorMessageLabel.setVisible(true);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            errorMessageLabel.setText("An error occurred: " + e.getMessage());
            errorMessageLabel.setVisible(true);
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        closeDialog();
    }
    
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}