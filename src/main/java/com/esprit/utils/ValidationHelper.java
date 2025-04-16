// Path: src/main/java/utils/ValidationHelper.java
package com.esprit.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ValidationHelper {
    
    private final Map<TextField, Label> fieldErrorMap = new HashMap<>();
    private boolean hasErrors = false;
    
    /**
     * Associates a field with its error label for validation
     * @param field The input field
     * @param errorLabel The label to display error messages
     * @return This ValidationHelper instance for chaining
     */
    public ValidationHelper addField(TextField field, Label errorLabel) {
        fieldErrorMap.put(field, errorLabel);
        // Initialize all error labels as hidden
        errorLabel.setVisible(false);
        return this;
    }
    
    /**
     * Reset all error labels
     */
    public void reset() {
        hasErrors = false;
        fieldErrorMap.forEach((field, label) -> {
            label.setVisible(false);
        });
    }
    
    /**
     * Validate that a field is not empty
     * @param field The field to validate
     * @param message The error message to display
     * @return True if valid, false otherwise
     */
    public boolean validateRequired(TextField field, String message) {
        boolean isValid = !field.getText().trim().isEmpty();
        if (!isValid) {
            showError(field, message);
            hasErrors = true;
        }
        return isValid;
    }
    
    /**
     * Validate a field against a custom predicate
     * @param field The field to validate
     * @param validator The validation predicate
     * @param message The error message to display
     * @return True if valid, false otherwise
     */
    public boolean validateField(TextField field, Predicate<String> validator, String message) {
        boolean isValid = validator.test(field.getText().trim());
        if (!isValid) {
            showError(field, message);
            hasErrors = true;
        }
        return isValid;
    }
    
    /**
     * Shows error message for a specific field
     * @param field The field with error
     * @param message The error message
     */
    public void showError(TextField field, String message) {
        Label errorLabel = fieldErrorMap.get(field);
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            hasErrors = true;
        }
    }
    
    /**
     * Check if validation found any errors
     * @return True if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return hasErrors;
    }
}
