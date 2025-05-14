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
    private final String captioningModel;
    private final double inappropriateThreshold;
    private final boolean fallbackEnabled;
    private final boolean asyncEnabled;
    private final boolean validationEnabled;

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
        this.captioningModel = config.getProperty("huggingface.captioning.model", "Salesforce/blip-image-captioning-large");
        this.inappropriateThreshold = Double.parseDouble(config.getProperty("huggingface.threshold", "0.5"));
        this.fallbackEnabled = Boolean.parseBoolean(config.getProperty("content.validation.fallback", "true"));
        this.asyncEnabled = Boolean.parseBoolean(config.getProperty("content.validation.async", "true"));
        this.validationEnabled = Boolean.parseBoolean(config.getProperty("content.validation.enabled", "true"));

        // Log which models are being used
        LOGGER.log(Level.INFO, "Initialized with models - Text: {0}, Violence: {1}, Image: {2}, Captioning: {3}, Threshold: {4}, Enabled: {5}",
                new Object[]{textModel, violenceModel, imageModel, captioningModel, inappropriateThreshold, validationEnabled});
    }

    private Properties loadConfiguration() {
        Properties props = new Properties();
        try {
            // First try to load from file system (root directory)
            File rootConfig = new File("C:\\xampp\\uniclubs\\config.properties");
            if (rootConfig.exists()) {
                props.load(new FileInputStream(rootConfig));
                LOGGER.log(Level.INFO, "AiValidationService: Loaded config from C:\\xampp\\uniclubs\\config.properties");
            } else {
                try {
                    // If not found in file system, try classpath
                props.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
                    LOGGER.log(Level.INFO, "AiValidationService: Loaded config from classpath");
            } catch (Exception e) {
                    // Final fallback to current directory
                props.load(new FileInputStream("config.properties"));
                    LOGGER.log(Level.INFO, "AiValidationService: Loaded config from current directory");
                }
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
        
        // Skip validation if globally disabled
        if (!validationEnabled) {
            LOGGER.log(Level.INFO, "Content validation disabled in config - skipping name validation");
            return new ValidationResult(true, "Valid name (validation disabled)", null);
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
            
            // Skip validation if globally disabled
            if (!validationEnabled) {
                LOGGER.log(Level.INFO, "Content validation disabled in config - skipping image validation");
                return new ValidationResult(true, "Valid image (validation disabled)", null);
            }
            
            // Check if API key is unavailable or empty
            if (huggingFaceClient.isApiKeyMissing()) {
                LOGGER.log(Level.WARNING, "HuggingFace API key is missing - bypassing image validation");
                return new ValidationResult(true, "Valid image (API key missing)", null);
            }
            
            // FIRST APPROACH: Direct NSFW detection
            JSONObject nsfwResult = huggingFaceClient.classifyImage(imageModel, imageFile);
            double nsfwScore = 0.0;
            
            if (nsfwResult != null && !nsfwResult.has("error")) {
                // Extract NSFW scores from model result
                if (nsfwResult.has("nsfw")) {
                    nsfwScore = nsfwResult.getDouble("nsfw");
                } else if (nsfwResult.has("unsafe") && nsfwResult.getDouble("unsafe") > nsfwScore) {
                    nsfwScore = nsfwResult.getDouble("unsafe");
                } else if (nsfwResult.has("porn") && nsfwResult.getDouble("porn") > nsfwScore) {
                    nsfwScore = nsfwResult.getDouble("porn");
                } else if (nsfwResult.has("sexy") && nsfwResult.getDouble("sexy") > nsfwScore) {
                    nsfwScore = nsfwResult.getDouble("sexy");
                }
                
                LOGGER.log(Level.INFO, "NSFW detection score: {0}", nsfwScore);
                
                // If we have a high NSFW score, immediately flag as inappropriate
                if (nsfwScore >= inappropriateThreshold) {
                    LOGGER.log(Level.WARNING, "NSFW content detected with score: {0}", nsfwScore);
                    return new ValidationResult(false, "This image contains inappropriate content", "NSFW score: " + nsfwScore);
                }
            } else {
                LOGGER.log(Level.WARNING, "NSFW detection failed: {0}", 
                        nsfwResult != null ? nsfwResult.optString("error", "Unknown error") : "Null result");
                // Continue to caption-based approach as fallback
            }
            
            // SECOND APPROACH: Caption-based detection (using model from config)
            JSONObject captionResult = huggingFaceClient.classifyImage(captioningModel, imageFile);
            
            if (captionResult == null || captionResult.has("error")) {
                String errorMsg = captionResult != null ? captionResult.getString("error") : "No response";
                LOGGER.log(Level.WARNING, "Image captioning failed: {0}", errorMsg);
                
                // If we already have NSFW detection, use that - otherwise we can't validate
                if (nsfwScore > 0) {
                    // We have some NSFW detection but below threshold
                    return new ValidationResult(true, "Valid image (based on NSFW score only)", null);
                }
                
                // Authentication errors should not trigger violations
                if (captionResult != null && captionResult.optString("error", "").contains("Invalid credentials")) {
                    LOGGER.log(Level.WARNING, "API authentication failed - not a content violation");
                    return new ValidationResult(true, "Valid image (auth error)", null);
                }
                
                // For other technical errors, reject but don't increment warning count
                return new ValidationResult(true, "Valid image (captioning failed)", null);
            }
            
            // Extract caption from various possible response formats
            String imageCaption = extractCaption(captionResult);
            
            if (imageCaption == null || imageCaption.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Could not extract caption from image response: {0}", captionResult.toString());
                
                // If we already have NSFW detection, use that
                if (nsfwScore > 0) {
                    return new ValidationResult(true, "Valid image (based on NSFW score only)", null);
                }
                
                return new ValidationResult(true, "Valid image (no caption extracted)", null);
            }
            
            LOGGER.log(Level.INFO, "Image caption: \"{0}\"", imageCaption);
            
            // Check caption for inappropriate content
            ValidationResult captionCheck = validateImageCaption(imageCaption);
            if (!captionCheck.isValid()) {
                return captionCheck;
            }
            
            // Image passed all checks
            return new ValidationResult(true, "Valid image", imageCaption);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during image validation", e);
            // Technical exceptions shouldn't count as violations
            return new ValidationResult(true, "Valid image (validation error)", null);
        }
    }
    
    /**
     * Extract caption from various possible response formats
     * @param captionResult The JSON response from the image captioning model
     * @return The extracted caption or null if none found
     */
    private String extractCaption(JSONObject captionResult) {
        try {
            // Try standard generated_text field
            if (captionResult.has("generated_text")) {
                return captionResult.getString("generated_text");
            }
            
            // Try caption field
            if (captionResult.has("caption")) {
                return captionResult.getString("caption");
            }
            
            // Try raw string response
            if (captionResult.toString().startsWith("\"") && captionResult.toString().endsWith("\"")) {
                return captionResult.toString().substring(1, captionResult.toString().length() - 1);
            }
            
            // Try results array
            if (captionResult.has("results") && captionResult.getJSONArray("results").length() > 0) {
                JSONArray results = captionResult.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    Object item = results.get(i);
                    if (item instanceof JSONObject) {
                        JSONObject resultObj = (JSONObject) item;
                        if (resultObj.has("generated_text")) {
                            return resultObj.getString("generated_text");
                        }
                    } else if (item instanceof String) {
                        return (String) item;
                    }
                }
            }
            
            // Try direct array response
            if (captionResult.toString().startsWith("[")) {
                JSONArray array = new JSONArray(captionResult.toString());
                if (array.length() > 0) {
                    return array.getString(0);
                }
            }
            
            // Dump full response for debugging
            LOGGER.log(Level.INFO, "Failed to extract caption from: {0}", captionResult.toString());
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extracting caption", e);
            return null;
        }
    }
    
    /**
     * Validate image caption for inappropriate content
     * @param caption The image caption to validate
     * @return ValidationResult with status and message
     */
    private ValidationResult validateImageCaption(String caption) {
        String lowercaseCaption = caption.toLowerCase();
        
        // Check for explicit NSFW terms
        String[] nsfwTerms = {
            "nude", "naked", "sex", "porn", "explicit", "nsfw", "xxx", 
            "adult content", "obscene", "sexual", "private parts", "genitals",
            "ass", "butt", "boobs", "penis", "vagina", "tits", "nipples", 
            "underwear", "bra", "panties", "bikini", "topless", "sexy", "erotic"
        };
        
        for (String term : nsfwTerms) {
            if (lowercaseCaption.contains(term)) {
                LOGGER.log(Level.WARNING, "NSFW term '{0}' detected in image caption: {1}", 
                        new Object[]{term, caption});
                return new ValidationResult(false, 
                        "This image appears to contain inappropriate content", caption);
            }
        }
        
        // Check for violence/gore terms
        String[] violenceTerms = {
            "blood", "gore", "violent", "dead body", "corpse", "murder", 
            "killed", "injury", "wounded", "graphic", "disturbing", 
            "cut", "wound", "severed", "dismembered", "mutilated", "gun", 
            "weapon", "knife", "stabbing", "shooting", "killed", "death", 
            "suicide", "terrorist", "war", "riot", "fight", "assault"
        };
        
        for (String term : violenceTerms) {
            if (lowercaseCaption.contains(term)) {
                LOGGER.log(Level.WARNING, "Violence term '{0}' detected in image caption: {1}", 
                        new Object[]{term, caption});
                return new ValidationResult(false, 
                        "This image appears to contain violent or graphic content", caption);
            }
        }
        
        // Also validate caption using text classification model
        try {
            JSONObject textResult = huggingFaceClient.classifyText(textModel, caption);
            if (textResult != null && !textResult.has("error")) {
                // Check all possible negative categories
                double offensiveScore = textResult.has("offensive") ? textResult.getDouble("offensive") : 0.0;
                double hateSpeechScore = textResult.has("hate speech") ? textResult.getDouble("hate speech") : 0.0;
                double inappropriateScore = textResult.has("inappropriate") ? textResult.getDouble("inappropriate") : 0.0;
                
                // Get highest negative score
                double highestNegativeScore = Math.max(Math.max(offensiveScore, hateSpeechScore), inappropriateScore);
                
                if (highestNegativeScore >= inappropriateThreshold) {
                    LOGGER.log(Level.WARNING, "Text model detected inappropriate content in image caption with score {0}: {1}", 
                            new Object[]{highestNegativeScore, caption});
                    return new ValidationResult(false, 
                            "This image description contains inappropriate content", caption);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error validating caption text", e);
            // Continue to other checks
        }
        
        // Caption passed all checks
        return new ValidationResult(true, "Valid image", caption);
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