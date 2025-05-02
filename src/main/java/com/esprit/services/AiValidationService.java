package com.esprit.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.esprit.utils.ProfanityFilter;

public class AiValidationService {

    private static final Logger LOGGER = Logger.getLogger(AiValidationService.class.getName());
    private final HuggingFaceClient huggingFaceClient;
    private final ProfanityFilter profanityFilter; // Fallback
    private final String textModel;
    private final String violenceModel;
    private final String imageModel;
    private final double inappropriateThreshold;
    private final boolean fallbackEnabled;
    private final boolean asyncEnabled;

    // List of common non-offensive names to allow
    private static final String[] COMMON_SAFE_NAMES = {
        "hello", "h", "he", "n", "no", "nour"
    };

    public AiValidationService() {
        this.huggingFaceClient = new HuggingFaceClient();
        this.profanityFilter = new ProfanityFilter();

        // Load configuration
        Properties config = loadConfiguration();
        this.textModel = config.getProperty("huggingface.text.model", "facebook/bart-large-mnli");
        this.violenceModel = config.getProperty("huggingface.violence.model", "Dabid/abusive-tagalog-profanity-detection");
        this.imageModel = config.getProperty("huggingface.image.model", "Falconsai/nsfw_image_detection");
        this.inappropriateThreshold = Double.parseDouble(config.getProperty("huggingface.threshold", "0.5"));
        this.fallbackEnabled = Boolean.parseBoolean(config.getProperty("content.validation.fallback", "true"));
        this.asyncEnabled = Boolean.parseBoolean(config.getProperty("content.validation.async", "true"));

        // Log which models are being used
        LOGGER.log(Level.INFO, "Initialized with models - Text: {0}, Violence: {1}, Image: {2}, Threshold: {3}",
                new Object[]{textModel, violenceModel, imageModel, inappropriateThreshold});
    }

    private Properties loadConfiguration() {
        Properties props = new Properties();
        try {
            // First try to load from classpath
            try {
                props.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            } catch (Exception e) {
                // If not found in classpath, try file system
                props.load(new FileInputStream("config.properties"));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load config.properties, using defaults", e);
        }
        return props;
    }

    /**
     * Validates a user's name using multiple AI models and falls back to
     * profanity filter if needed
     *
     * @param name The name to validate
     * @return ValidationResult with status and message
     */
    public ValidationResult validateName(String name) {
    if (name == null || name.trim().isEmpty()) {
        return new ValidationResult(true, "Valid name");
    }
    
    try {
        LOGGER.log(Level.INFO, "Starting validation for name: {0}", name);
        
        // First check using profanity filter as a quick check
        if (profanityFilter.containsProfanity(name)) {
            LOGGER.log(Level.WARNING, "Profanity filter detected inappropriate content in \"{0}\"", name);
            return new ValidationResult(false, "This name contains inappropriate language");
        }
        
        // Text model classification - with proper candidate labels
        JSONObject result = huggingFaceClient.classifyText(textModel, name);
        
        // Dump full JSON response for debugging
        LOGGER.log(Level.INFO, "Full JSON response for {0}: {1}", 
                new Object[]{name, result != null ? result.toString() : "null"});
        
        // Check all harmful categories from the model results
        if (result != null && !result.has("error")) {
            // Check all possible negative categories
            double offensiveScore = result.has("offensive") ? result.getDouble("offensive") : 0.0;
            double hateSpeechScore = result.has("hate speech") ? result.getDouble("hate speech") : 0.0;
            double inappropriateScore = result.has("inappropriate") ? result.getDouble("inappropriate") : 0.0;
            
            // Log all scores for debugging
            LOGGER.log(Level.INFO, "Scores for {0}: offensive={1}, hate_speech={2}, inappropriate={3}", 
                    new Object[]{name, offensiveScore, hateSpeechScore, inappropriateScore});
            
            // Get the highest score among negative categories
            double highestNegativeScore = Math.max(Math.max(offensiveScore, hateSpeechScore), inappropriateScore);
            
            // If any negative category exceeds threshold
            if (highestNegativeScore >= inappropriateThreshold) {
                String category = "inappropriate content";
                if (offensiveScore >= inappropriateThreshold) category = "offensive content";
                if (hateSpeechScore >= inappropriateThreshold) category = "hate speech";
                
                LOGGER.log(Level.WARNING, "Text model detected {0} in \"{1}\" with score {2} (threshold: {3})", 
                        new Object[]{category, name, highestNegativeScore, inappropriateThreshold});
                return new ValidationResult(false, "This name contains " + category);
            }
            
            // If appropriate score is higher than all negative scores, mark as valid
            double appropriateScore = result.has("appropriate") ? result.getDouble("appropriate") : 0.0;
            if (appropriateScore > 0 && appropriateScore > highestNegativeScore) {
                LOGGER.log(Level.INFO, "Name \"{0}\" classified as appropriate with score {1}", 
                        new Object[]{name, appropriateScore});
                return new ValidationResult(true, "Valid name");
            }
            
            // If we got here and have valid results, consider it safe
            if (highestNegativeScore < inappropriateThreshold) {
                LOGGER.log(Level.INFO, "Text model validation passed for \"{0}\"", name);
                return new ValidationResult(true, "Valid name");
            }
        } else {
            LOGGER.log(Level.WARNING, "Text model check failed: {0}", 
                    result != null ? result.optString("error", "Unknown error") : "Null result");
            // Fall back to violence model
        }
        
        // Only proceed to violence model if text model didn't give a clear result
        JSONObject violenceResult = huggingFaceClient.classifyText(violenceModel, name);
        
        if (violenceResult != null && !violenceResult.has("error")) {
            // Extract violence score 
            double abusiveScore = 0.0;
            
            // For specialized profanity detection model, use the abusive score
            if (violenceResult.has("abusive")) {
                abusiveScore = violenceResult.getDouble("abusive");
            } else if (violenceResult.has("LABEL_1")) {
                // Some models use LABEL_1 for the abusive category
                abusiveScore = violenceResult.getDouble("LABEL_1");
            }
            
            LOGGER.log(Level.INFO, "Violence model score for {0}: {1} (threshold: {2})", 
                    new Object[]{name, abusiveScore, inappropriateThreshold});
            
            // Check against threshold
            if (abusiveScore >= inappropriateThreshold) {
                LOGGER.log(Level.WARNING, "Violence model detected abusive content in \"{0}\" with score {1} (threshold: {2})", 
                        new Object[]{name, abusiveScore, inappropriateThreshold});
                return new ValidationResult(false, "This name contains abusive language");
            }
            
            // If passed the checks, content is appropriate
            LOGGER.log(Level.INFO, "Name \"{0}\" passed all validation checks", name);
            return new ValidationResult(true, "Valid name");
        } else {
            LOGGER.log(Level.WARNING, "Violence model check failed: {0}",
                    violenceResult != null ? violenceResult.optString("error", "Unknown error") : "Null result");
            // Fall back to profanity filter
            return fallbackToProfanityFilter(name);
        }
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error during name validation", e);
        // Fall back to profanity filter
        return fallbackToProfanityFilter(name);
    }
}

    /**
     * Check if a name is in the list of common safe names
     */
    private boolean isCommonSafeName(String name) {
        if (name == null) {
            return false;
        }

        String nameLower = name.toLowerCase().trim();
        for (String safeName : COMMON_SAFE_NAMES) {
            if (safeName.equalsIgnoreCase(nameLower)) {
                return true;
            }
        }
        return false;
    }

    private ValidationResult fallbackToProfanityFilter(String text) {
        if (!fallbackEnabled) {
            return new ValidationResult(true, "Valid text"); // Default to accepting if fallback disabled
        }

        LOGGER.log(Level.INFO, "AI validation failed, falling back to profanity filter");
        if (profanityFilter.containsProfanity(text)) {
            return new ValidationResult(false, "This text contains inappropriate words");
        }
        return new ValidationResult(true, "Valid text");
    }

    /**
     * Asynchronously validates a user's name
     *
     * @param name The name to validate
     * @return CompletableFuture with validation result
     */
    public CompletableFuture<ValidationResult> validateNameAsync(String name) {
        if (!asyncEnabled) {
            // Run synchronously if async is disabled
            ValidationResult result = validateName(name);
            return CompletableFuture.completedFuture(result);
        }
        return CompletableFuture.supplyAsync(() -> validateName(name));
    }

    /**
     * Validates a profile image using AI
     *
     * @param imageFile The image file to validate
     * @return ValidationResult with status and message
     */
    public ValidationResult validateProfileImage(File imageFile) {
        try {
            JSONObject result = huggingFaceClient.classifyImage(imageModel, imageFile);

            if (result != null) {
                // Check if there was an error
                if (result.has("error")) {
                    LOGGER.log(Level.WARNING, "AI image validation error: {0}", result.getString("error"));
                    return new ValidationResult(false, "Unable to validate image content, please try another image");
                }

                double nsfwScore = 0.0;

                // Try different fields that might contain NSFW/adult content score
                if (result.has("nsfw")) {
                    nsfwScore = result.getDouble("nsfw");
                } else if (result.has("adult")) {
                    nsfwScore = result.getDouble("adult");
                } else if (result.has("porn")) {
                    nsfwScore = result.getDouble("porn");
                } else if (result.has("unsafe")) {
                    nsfwScore = result.getDouble("unsafe");
                }

                if (nsfwScore > inappropriateThreshold) {
                    LOGGER.log(Level.INFO, "Image model {0} detected inappropriate content with score {1}",
                            new Object[]{imageModel, nsfwScore});
                    return new ValidationResult(false, "This image may contain inappropriate content");
                }
                return new ValidationResult(true, "Valid image");
            } else {
                // No fallback for images, use a conservative approach
                LOGGER.log(Level.WARNING, "Image validation failed, using conservative approach");
                return new ValidationResult(false, "Unable to validate image content, please try another image");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during image validation", e);
            return new ValidationResult(false, "Error validating image, please try another one");
        }
    }

    /**
     * Asynchronously validates a profile image
     *
     * @param imageFile The image file to validate
     * @return CompletableFuture with validation result
     */
    public CompletableFuture<ValidationResult> validateProfileImageAsync(File imageFile) {
        if (!asyncEnabled) {
            // Run synchronously if async is disabled
            ValidationResult result = validateProfileImage(imageFile);
            return CompletableFuture.completedFuture(result);
        }
        return CompletableFuture.supplyAsync(() -> validateProfileImage(imageFile));
    }

    // Value class for validation results
    public static class ValidationResult {

        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
