package com.esprit.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public class HuggingFaceClient {
    private static final Logger LOGGER = Logger.getLogger(HuggingFaceClient.class.getName());
    private static final String HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/";
    private final String apiKey;
    private final HttpClient httpClient;
    private final ContentValidationCache cache;
    
    public HuggingFaceClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = loadApiKey();
        this.cache = ContentValidationCache.getInstance();
    }
    
    private String loadApiKey() {
        try {
            Properties properties = new Properties();
            try {
                // First try to load from classpath
                properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            } catch (Exception e) {
                // If not found in classpath, try file system
                properties.load(new FileInputStream("config.properties"));
            }
            String key = properties.getProperty("huggingface.api.key");
            if (key == null || key.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Hugging Face API key not found in config.properties");
                return "";
            }
            return key;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading Hugging Face API key", e);
            return "";
        }
    }
    
    /**
     * Escapes special characters in a string for use in JSON
     * @param input The string to escape
     * @return The escaped string
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '/':
                    escaped.append("\\/");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        String hex = Integer.toHexString(c);
                        escaped.append("\\u");
                        for (int j = 0; j < 4 - hex.length(); j++) {
                            escaped.append('0');
                        }
                        escaped.append(hex);
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }
    
    /**
     * Sends a text to a Hugging Face model for classification
     * @param model The model ID to use
     * @param text The text to classify
     * @return JSONObject containing the model's response or null if failed
     */
    public JSONObject classifyText(String model, String text) {
        if (text == null || text.trim().isEmpty()) {
            JSONObject errorObject = new JSONObject();
            errorObject.put("error", "Empty text");
            return errorObject;
        }
        
        try {
            // Generate a cache key
            String cacheKey = model + "_" + text.hashCode();
            
            // Check cache first
            JSONObject cachedResult = cache.get(cacheKey);
            if (cachedResult != null) {
                LOGGER.log(Level.INFO, "Using cached result for: {0}", 
                        text.substring(0, Math.min(text.length(), 50)));
                return cachedResult;
            }
            
            LOGGER.log(Level.INFO, "Sending text to model: {0}", 
                    text.substring(0, Math.min(text.length(), 50)));
            
            // Create the payload based on model type
            String payload;
            if (model.contains("bart-large-mnli")) {
                // Format for zero-shot classification models that require candidate_labels
                JSONObject requestObj = new JSONObject();
                requestObj.put("inputs", text);
                // Add required candidate_labels for zero-shot classification
                JSONArray candidateLabels = new JSONArray();
                candidateLabels.put("appropriate");
                candidateLabels.put("inappropriate");
                candidateLabels.put("hate speech");
                candidateLabels.put("offensive");
                requestObj.put("parameters", new JSONObject().put("candidate_labels", candidateLabels));
                requestObj.put("options", new JSONObject().put("wait_for_model", true));
                payload = requestObj.toString();
            } else {
                // Default format for other models
                payload = "{\"inputs\": \"" + escapeJsonString(text) + "\", \"options\": {\"wait_for_model\": true}}";
            }
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HUGGING_FACE_API_URL + model))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Only log full response on debug or if error
            if (response.statusCode() != 200) {
                LOGGER.log(Level.WARNING, "API request failed with status {0}: {1}", 
                    new Object[]{response.statusCode(), response.body()});
            }
            
            JSONObject resultObject = null;
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                LOGGER.log(Level.INFO, "API response received, length: {0}", responseBody.length());
                
                // Handle nested array response format [[{...},{...}]]
                if (responseBody.trim().startsWith("[[")) {
                    try {
                        JSONArray outerArray = new JSONArray(responseBody);
                        if (outerArray.length() > 0) {
                            JSONArray innerArray = outerArray.getJSONArray(0);
                            resultObject = new JSONObject();
                            
                            // Store all label/score pairs at the top level for easier access
                            for (int i = 0; i < innerArray.length(); i++) {
                                JSONObject item = innerArray.getJSONObject(i);
                                if (item.has("label") && item.has("score")) {
                                    String label = item.getString("label").toLowerCase();
                                    double score = item.getDouble("score");
                                    
                                    // Store all relevant scores at top level for easier access
                                    resultObject.put(label, score);
                                    
                                    // Log all scores for debugging
                                    LOGGER.log(Level.FINE, "Score for {0}: {1}", new Object[]{label, score});
                                }
                            }
                            
                            // Store original result for reference
                            resultObject.put("originalResult", innerArray);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parsing nested array response: {0}", e.getMessage());
                        resultObject = new JSONObject();
                        resultObject.put("error", "Error parsing response: " + e.getMessage());
                        resultObject.put("rawResponse", responseBody);
                    }
                } 
                // Handle zero-shot classification output (common for BART model)
                else if (responseBody.contains("scores") && responseBody.contains("labels")) {
                    try {
                        resultObject = new JSONObject(responseBody);
                        
                        // For BART model, make sure inappropriate/hate scores are properly extracted
                        if (resultObject.has("labels") && resultObject.has("scores")) {
                            JSONArray labels = resultObject.getJSONArray("labels");
                            JSONArray scores = resultObject.getJSONArray("scores");
                            
                            // Map each label and score to top level for easier access
                            for (int i = 0; i < labels.length(); i++) {
                                String label = labels.getString(i).toLowerCase();
                                double score = scores.getDouble(i);
                                resultObject.put(label, score);
                                
                                // Special handling for inappropriate content
                                if (label.contains("inappropriate") || 
                                    label.contains("hate speech") || 
                                    label.contains("offensive")) {
                                    // Track as potential toxic content
                                    resultObject.put("inappropriateScore", score);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parsing BART response: {0}", e.getMessage());
                        resultObject = new JSONObject();
                        resultObject.put("error", "Error parsing response: " + e.getMessage());
                        resultObject.put("rawResponse", responseBody);
                    }
                }
                // Check if response is an array or object
                else if (responseBody.trim().startsWith("[")) {
                    // Handle array response
                    JSONArray jsonArray = new JSONArray(responseBody);
                    resultObject = new JSONObject();
                    resultObject.put("results", jsonArray);
                    
                    // Handle array of objects with labels/scores
                    if (jsonArray.length() > 0) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            if (jsonArray.get(i) instanceof JSONObject) {
                                JSONObject item = jsonArray.getJSONObject(i);
                                // For BART model, handle the specific format
                                if (item.has("labels") && item.has("scores")) {
                                    JSONArray labels = item.getJSONArray("labels");
                                    JSONArray scores = item.getJSONArray("scores");
                                    
                                    for (int j = 0; j < labels.length(); j++) {
                                        String label = labels.getString(j).toLowerCase();
                                        double score = scores.getDouble(j);
                                        
                                        // Add to top level for easier access
                                        resultObject.put(label, score);
                                        
                                        // Log all scores for debugging
                                        LOGGER.log(Level.FINE, "Score for {0}: {1}", new Object[]{label, score});
                                    }
                                }
                            }
                        }
                    }
                } 
                else if (responseBody.trim().startsWith("{")) {
                    // Standard JSON object response
                    resultObject = new JSONObject(responseBody);
                } 
                else {
                    // Handle unexpected response format
                    LOGGER.log(Level.WARNING, "Unexpected response format: {0}", responseBody);
                    resultObject = new JSONObject();
                    resultObject.put("error", "Unexpected response format");
                    resultObject.put("rawResponse", responseBody);
                }
                
                // Cache the result for future use
                if (resultObject != null && !resultObject.has("error")) {
                    cache.put(cacheKey, resultObject, 60, java.util.concurrent.TimeUnit.MINUTES);
                }
            } 
            else if (response.statusCode() == 503) {
                // Model is loading
                LOGGER.log(Level.FINE, "Model is loading, response: {0}", response.body());
                resultObject = new JSONObject();
                resultObject.put("error", "Model is loading, please try again later");
            } 
            else {
                resultObject = new JSONObject();
                resultObject.put("error", "API request failed with status: " + response.statusCode());
            }
            
            return resultObject != null ? resultObject : new JSONObject().put("error", "No response");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calling Hugging Face API", e);
            JSONObject errorObject = new JSONObject();
            errorObject.put("error", "API call exception: " + e.getMessage());
            return errorObject;
        }
    }
    
    /**
     * Sends an image to a Hugging Face model for classification
     * @param model The model ID to use
     * @param imageFile The image file to classify
     * @return JSONObject containing the model's response or null if failed
     */
    public JSONObject classifyImage(String model, File imageFile) {
        try {
            // Create cache key based on model and file
            String cacheKey = model + "_" + imageFile.getAbsolutePath() + "_" + imageFile.lastModified();
            
            // Check cache first
            JSONObject cachedResult = cache.get(cacheKey);
            if (cachedResult != null) {
                LOGGER.log(Level.INFO, "Using cached result for image: {0}", imageFile.getName());
                return cachedResult;
            }
            
            // Minimal logging
            LOGGER.log(Level.FINE, "Classifying image with model: {0}", model);
            
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            
            // Create payload based on model requirements
            String payload;
            if (model.contains("nsfw_image_detection")) {
                // Format for nsfw detection model
                JSONObject requestObj = new JSONObject();
                requestObj.put("inputs", base64Image);
                requestObj.put("options", new JSONObject().put("wait_for_model", true));
                payload = requestObj.toString();
            } else {
                // Default format
                JSONObject requestObj = new JSONObject();
                requestObj.put("inputs", base64Image);
                payload = requestObj.toString();
            }
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HUGGING_FACE_API_URL + model))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Only log on error
            if (response.statusCode() != 200) {
                LOGGER.log(Level.WARNING, "API image request failed with status {0}: {1}", 
                    new Object[]{response.statusCode(), response.body()});
            }
            
            JSONObject resultObject = null;
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                
                // Check if response is an array or object
                if (responseBody.trim().startsWith("[")) {
                    // Handle array response
                    JSONArray jsonArray = new JSONArray(responseBody);
                    resultObject = new JSONObject();
                    resultObject.put("results", jsonArray);
                    
                    // Extract and add nsfw scores to top level
                    if (jsonArray.length() > 0) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            if (jsonArray.get(i) instanceof JSONObject) {
                                JSONObject item = jsonArray.getJSONObject(i);
                                // Extract key scores to top level
                                for (String key : new String[]{"nsfw", "adult", "porn", "unsafe"}) {
                                    if (item.has(key)) {
                                        double score = item.getDouble(key);
                                        resultObject.put(key, score);
                                    }
                                }
                            }
                        }
                    }
                } else if (responseBody.trim().startsWith("{")) {
                    // Standard JSON object response
                    resultObject = new JSONObject(responseBody);
                } else {
                    // Handle unexpected response format
                    LOGGER.log(Level.WARNING, "Unexpected image response format: {0}", responseBody);
                    resultObject = new JSONObject();
                    resultObject.put("error", "Unexpected response format");
                    resultObject.put("rawResponse", responseBody);
                }
                
                // Cache the result for future use
                if (resultObject != null && !resultObject.has("error")) {
                    cache.put(cacheKey, resultObject, 60, java.util.concurrent.TimeUnit.MINUTES);
                }
            } else if (response.statusCode() == 503) {
                // Model is loading
                LOGGER.log(Level.FINE, "Model is loading, response: {0}", response.body());
                resultObject = new JSONObject();
                resultObject.put("error", "Model is loading, please try again later");
            } else {
                resultObject = new JSONObject();
                resultObject.put("error", "API request failed with status: " + response.statusCode());
            }
            
            return resultObject;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calling Hugging Face API for image", e);
            JSONObject errorObject = new JSONObject();
            errorObject.put("error", "API call exception: " + e.getMessage());
            return errorObject;
        }
    }
}