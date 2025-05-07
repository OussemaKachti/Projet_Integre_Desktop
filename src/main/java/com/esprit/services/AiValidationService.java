package com.esprit.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public class AiValidationService {

    private static final Logger LOGGER = Logger.getLogger(AiValidationService.class.getName());
    private final HuggingFaceClient huggingFaceClient;
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
            return new ValidationResult(true, "Valid name", null);
        }
        
        try {
            LOGGER.log(Level.INFO, "Starting validation for name: {0}", name);
            
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
                double appropriateScore = result.has("appropriate") ? result.getDouble("appropriate") : 0.0;
                
                // More stringent check: if any negative category is close to appropriate score (within 10%)
                // or exceeds threshold, reject
                if (highestNegativeScore >= inappropriateThreshold || 
                    (appropriateScore > 0 && highestNegativeScore >= (appropriateScore * 0.9))) {
                    String category = "inappropriate content";
                    if (offensiveScore >= inappropriateThreshold || offensiveScore >= (appropriateScore * 0.9)) 
                        category = "offensive content";
                    if (hateSpeechScore >= inappropriateThreshold || hateSpeechScore >= (appropriateScore * 0.9)) 
                        category = "hate speech";
                    
                    LOGGER.log(Level.WARNING, "Text model detected {0} in \"{1}\" with score {2} (threshold: {3}, appropriate: {4})", 
                            new Object[]{category, name, highestNegativeScore, inappropriateThreshold, appropriateScore});
                    return new ValidationResult(false, "This name contains " + category, null);
                }
                
                // If appropriate score is significantly higher than all negative scores, mark as valid
                if (appropriateScore > 0 && appropriateScore > (highestNegativeScore * 1.2)) {
                    LOGGER.log(Level.INFO, "Name \"{0}\" classified as appropriate with score {1}", 
                            new Object[]{name, appropriateScore});
                    return new ValidationResult(true, "Valid name", null);
                }
                
                // If we got here and have valid results with low negative scores, consider it safe
                if (highestNegativeScore < inappropriateThreshold && highestNegativeScore < 0.3) {
                    LOGGER.log(Level.INFO, "Text model validation passed for \"{0}\"", name);
                    return new ValidationResult(true, "Valid name", null);
                } else {
                    // If we're not confident enough, reject
                    LOGGER.log(Level.WARNING, "Not confident enough in validation for \"{0}\"", name);
                    return new ValidationResult(false, "This name may contain inappropriate content", null);
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
                
                // Check against threshold - be more stringent here too
                if (abusiveScore >= inappropriateThreshold * 0.8) {
                    LOGGER.log(Level.WARNING, "Violence model detected abusive content in \"{0}\" with score {1} (threshold: {2})", 
                            new Object[]{name, abusiveScore, inappropriateThreshold});
                    return new ValidationResult(false, "This name contains abusive language", null);
                }
                
                // If passed the checks, content is appropriate
                LOGGER.log(Level.INFO, "Name \"{0}\" passed all validation checks", name);
                return new ValidationResult(true, "Valid name", null);
            } else {
                LOGGER.log(Level.WARNING, "Violence model check failed: {0}",
                        violenceResult != null ? violenceResult.optString("error", "Unknown error") : "Null result");
                // Don't fall back to profanity filter here to prevent double-counting
                return new ValidationResult(true, "Valid name", null);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during name validation", e);
            // Don't fall back to profanity filter to prevent double-counting
            return new ValidationResult(true, "Valid name", null);
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
        // We've completely removed the profanity filter to prevent double warning counts
        return new ValidationResult(true, "Valid text", null);
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
            LOGGER.log(Level.INFO, "Starting image validation for file: {0}", imageFile.getName());
            
            // Step 1: Generate image caption using the image captioning model
            JSONObject captionResult = huggingFaceClient.classifyImage(imageModel, imageFile);
            
            if (captionResult == null || captionResult.has("error")) {
                String errorMsg = captionResult != null ? captionResult.getString("error") : "No response";
                LOGGER.log(Level.WARNING, "Image captioning failed: {0}", errorMsg);
                return new ValidationResult(false, "Unable to verify image content. Please try another image.", null);
            }
            
            // Extract caption from the model response
            String imageCaption = "";
            if (captionResult.has("generated_text")) {
                imageCaption = captionResult.getString("generated_text");
            } else if (captionResult.has("caption")) {
                imageCaption = captionResult.getString("caption");
            } else if (captionResult.has("results") && captionResult.getJSONArray("results").length() > 0) {
                // Try to extract from results array
                JSONArray results = captionResult.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    Object item = results.get(i);
                    if (item instanceof JSONObject) {
                        JSONObject resultObj = (JSONObject) item;
                        if (resultObj.has("generated_text")) {
                            imageCaption = resultObj.getString("generated_text");
                            break;
                        }
                    } else if (item instanceof String) {
                        imageCaption = (String) item;
                        break;
                    }
                }
            }
            
            // Check if caption was successfully extracted
            if (imageCaption == null || imageCaption.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Could not extract caption from image");
                return new ValidationResult(false, "Unable to analyze image content. Please try another image.", null);
            }
            
            LOGGER.log(Level.INFO, "Image caption generated: {0}", imageCaption);
            
            // Step 2: Analyze caption for inappropriate content
            // Check for explicit NSFW terms
            String lowercaseCaption = imageCaption.toLowerCase();
            String[] nsfwTerms = {"nude", "naked", "sex", "porn", "explicit", "nsfw", "xxx", 
                                 "adult content", "obscene", "sexual", "private parts"};
            
            for (String term : nsfwTerms) {
                if (lowercaseCaption.contains(term)) {
                    LOGGER.log(Level.WARNING, "NSFW term '{0}' detected in image caption", term);
                    return new ValidationResult(false, "This image appears to contain inappropriate content.", imageCaption);
                }
            }
            
            // Check for violence/gore terms
            String[] violenceTerms = {"blood", "gore", "violent", "dead body", "corpse", "murder", 
                                     "killed", "injury", "wounded", "graphic", "disturbing", 
                                     "cut", "wound", "severed", "dismembered", "mutilated"};
            
            for (String term : violenceTerms) {
                if (lowercaseCaption.contains(term)) {
                    LOGGER.log(Level.WARNING, "Violence/gore term '{0}' detected in image caption", term);
                    return new ValidationResult(false, "This image appears to contain violent or graphic content.", imageCaption);
                }
            }
            
            // Step 3: Double-check with text model for safety
            JSONObject textResult = huggingFaceClient.classifyText(textModel, 
                    "Is this image safe and appropriate as a profile picture: " + imageCaption);
            
            if (textResult != null && !textResult.has("error")) {
                // Look for inappropriate signals
                double unsafeScore = 0.0;
                
                if (textResult.has("inappropriate")) {
                    unsafeScore = Math.max(unsafeScore, textResult.getDouble("inappropriate"));
                }
                if (textResult.has("unsafe")) {
                    unsafeScore = Math.max(unsafeScore, textResult.getDouble("unsafe"));
                }
                if (textResult.has("violence")) {
                    unsafeScore = Math.max(unsafeScore, textResult.getDouble("violence"));
                }
                if (textResult.has("adult")) {
                    unsafeScore = Math.max(unsafeScore, textResult.getDouble("adult"));
                }
                
                LOGGER.log(Level.INFO, "Text analysis of caption - unsafe score: {0}", unsafeScore);
                
                if (unsafeScore > inappropriateThreshold) {
                    return new ValidationResult(false, "This image may not be appropriate for a profile picture.", imageCaption);
                }
            }
            
            // Image passed all checks
            LOGGER.log(Level.INFO, "Image passed all validation checks");
            return new ValidationResult(true, "Valid image", imageCaption);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during image validation", e);
            return new ValidationResult(false, "Error validating image. Please try another one.", null);
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
        private final String imageCaption;

        public ValidationResult(boolean valid, String message, String imageCaption) {
            this.valid = valid;
            this.message = message;
            this.imageCaption = imageCaption;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
        
        public String getImageCaption() {
            return imageCaption;
        }
    }
}