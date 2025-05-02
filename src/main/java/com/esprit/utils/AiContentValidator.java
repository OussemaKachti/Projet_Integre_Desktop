package com.esprit.utils;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.esprit.services.AiValidationService;

import javafx.application.Platform;

/**
 * Utility class with static methods for easy validation from controllers
 */
public class AiContentValidator {
    private static final Logger LOGGER = Logger.getLogger(AiContentValidator.class.getName());
    private static final AiValidationService aiService = new AiValidationService();
    
    // Constants to define minimum lengths for validation
    private static final int MIN_LENGTH_FOR_VALIDATION = 2;
    
    /**
     * Validates if a name contains appropriate content
     * @param name The name to validate
     * @return True if the name is appropriate, false otherwise
     */
    public static boolean isAppropriateContent(String name) {
        // Skip very short inputs
        if (name == null || name.trim().length() < MIN_LENGTH_FOR_VALIDATION) {
            return true;
        }
        
        LOGGER.log(Level.INFO, "Starting synchronous validation for: {0}", name);
        AiValidationService.ValidationResult result = aiService.validateName(name);
        if (!result.isValid()) {
            LOGGER.log(Level.INFO, "Synchronous content validation failed: {0} for text: {1}", 
                    new Object[]{result.getMessage(), name});
        } else {
            LOGGER.log(Level.INFO, "Synchronous content validation passed for: {0}", name);
        }
        return result.isValid();
    }
    
    /**
     * Validates if a name contains appropriate content with a callback
     * @param name The name to validate
     * @param callback The callback to execute after validation
     */
    public static void validateNameAsync(String name, ValidationCallback callback) {
        // Skip validation for very short inputs
        if (name == null || name.trim().length() < MIN_LENGTH_FOR_VALIDATION) {
            if (callback != null) {
                Platform.runLater(() -> callback.onValidationComplete(true, "Valid name"));
            }
            return;
        }
        
        CompletableFuture<AiValidationService.ValidationResult> future = aiService.validateNameAsync(name);
        future.thenAccept(result -> {
            Platform.runLater(() -> {
                if (callback != null) {
                    // Log validation outcome for debugging
                    if (!result.isValid()) {
                        LOGGER.log(Level.INFO, "Async validation failed for text: {0} - {1}", 
                                new Object[]{name, result.getMessage()});
                    }
                    callback.onValidationComplete(result.isValid(), result.getMessage());
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (callback != null) {
                    LOGGER.log(Level.WARNING, "Validation error: {0}", ex.getMessage());
                    // Default to accepting on error to not block user registration
                    callback.onValidationComplete(true, "Valid name");
                }
            });
            return null;
        });
    }
    
    /**
     * Validates if an image contains appropriate content
     * @param imageFile The image file to validate
     * @return True if the image is appropriate, false otherwise
     */
    public static boolean isAppropriateImage(File imageFile) {
        if (imageFile == null || !imageFile.exists() || imageFile.length() <= 0) {
            return false;
        }
        
        AiValidationService.ValidationResult result = aiService.validateProfileImage(imageFile);
        if (!result.isValid()) {
            LOGGER.log(Level.INFO, "Image validation failed: {0}", result.getMessage());
        }
        return result.isValid();
    }
    
    /**
     * Validates if an image contains appropriate content with a callback
     * @param imageFile The image file to validate
     * @param callback The callback to execute after validation
     */
    public static void validateImageAsync(File imageFile, ValidationCallback callback) {
        if (imageFile == null || !imageFile.exists() || imageFile.length() <= 0) {
            if (callback != null) {
                Platform.runLater(() -> callback.onValidationComplete(false, "Invalid image file"));
            }
            return;
        }
        
        CompletableFuture<AiValidationService.ValidationResult> future = aiService.validateProfileImageAsync(imageFile);
        future.thenAccept(result -> {
            Platform.runLater(() -> {
                if (callback != null) {
                    if (!result.isValid()) {
                        LOGGER.log(Level.INFO, "Async image validation failed: {0}", result.getMessage());
                    }
                    callback.onValidationComplete(result.isValid(), result.getMessage());
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (callback != null) {
                    LOGGER.log(Level.WARNING, "Image validation error: {0}", ex.getMessage());
                    callback.onValidationComplete(false, "Image validation error: " + ex.getMessage());
                }
            });
            return null;
        });
    }
    
    /**
     * Callback interface for async validation
     */
    public interface ValidationCallback {
        void onValidationComplete(boolean isValid, String message);
    }
}