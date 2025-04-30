package com.esprit.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service for detecting toxic content in text using OpenAI API
 */
public class AiService {
    private String apiKey;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    
    public AiService() {
        // Get API key from environment variables or configuration
        apiKey = System.getenv("OPENAI_API_KEY");
        
        // If not found in environment variables, try from ConfigurationManager
        if (apiKey == null || apiKey.isEmpty()) {
            ConfigurationManager config = ConfigurationManager.getInstance();
            apiKey = config.getProperty("OPENAI_API_KEY");
        }
        
        // If still not found, log a warning but don't fail (we'll handle missing key in isToxic method)
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Warning: OpenAI API key not found. Toxicity detection will use fallback method.");
        }
    }
    
    /**
     * Analyzes text for toxic content using OpenAI API
     * 
     * @param text Text to analyze
     * @return JSON object with toxicity analysis results
     */
    public JSONObject analyzeToxicity(String text) {
        if (apiKey == null || apiKey.isEmpty()) {
            // Fallback to basic analysis if no API key is available
            return fallbackToxicityAnalysis(text);
        }
        
        try {
            // Create the request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("temperature", 0.3);
            
            JSONArray messages = new JSONArray();
            
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a content moderator. Analyze the following text for toxic content, profanity, or inappropriate language. Respond with a JSON object containing: isToxic (boolean), toxicWords (array of toxic words found), and reason (explanation).");
            messages.put(systemMessage);
            
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", text);
            messages.put(userMessage);
            
            requestBody.put("messages", messages);
            
            // Make the API request
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Check response code first to handle auth errors better
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorMessage;
                switch (responseCode) {
                    case 401:
                        errorMessage = "Authentication error: Your API key is invalid.";
                        break;
                    case 403:
                        errorMessage = "Authorization error: Your API key might be correct, but doesn't have the right permissions. " +
                                      "Make sure you're using a proper OpenAI API key that starts with 'sk-' (not 'sk-proj-') " +
                                      "and your account has billing enabled.";
                        break;
                    case 429:
                        errorMessage = "Rate limit exceeded: You've exceeded your API quota or rate limits.";
                        break;
                    default:
                        errorMessage = "Error from OpenAI API: HTTP response code: " + responseCode;
                }
                System.err.println(errorMessage);
                return fallbackToxicityAnalysis(text);
            }
            
            // If we get here, read the successful response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            
            // Parse the response
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                String content = message.getString("content");
                
                // Parse the content as JSON
                try {
                    return new JSONObject(content);
                } catch (Exception e) {
                    // If parsing fails, create a simplified response
                    System.err.println("Failed to parse AI response as JSON: " + content);
                    return createSimpleToxicityResponse(false, new ArrayList<>(), "Analysis completed but response format was invalid");
                }
            }
            
            return createSimpleToxicityResponse(false, new ArrayList<>(), "Analysis failed");
            
        } catch (Exception e) {
            e.printStackTrace();
            return fallbackToxicityAnalysis(text);
        }
    }
    
    /**
     * Simple method to check if text is toxic without using OpenAI API
     * 
     * @param text Text to check
     * @return True if text contains toxic words, false otherwise
     */
    public boolean isToxic(String text) {
        JSONObject analysis = analyzeToxicity(text);
        return analysis.getBoolean("isToxic");
    }
    
    /**
     * Basic fallback analysis for when API is not available
     */
    private JSONObject fallbackToxicityAnalysis(String text) {
        if (text == null) {
            return createSimpleToxicityResponse(false, new ArrayList<>(), "Empty text");
        }
        
        String lowerText = text.toLowerCase();
        List<String> toxicWords = new ArrayList<>();
        
        // List of potentially toxic or inappropriate words
        String[] badWords = {
            "insulte", "grossier", "offensive", "vulgar", "idiot", "stupid",
            "damn", "hell", "ass", "crap", "shit", "fuck", "bitch", "bastard",
            "asshole", "dick", "whore", "slut", "nigger", "faggot", "retard",
            "porn", "pornography", "anal", "sex", "penis", "vagina", "pussy"
        };
        
        for (String word : badWords) {
            if (lowerText.contains(word)) {
                toxicWords.add(word);
            }
        }
        
        boolean isToxic = !toxicWords.isEmpty();
        String reason = isToxic ? 
                "The text contains potentially inappropriate language." : 
                "No toxic content detected.";
        
        return createSimpleToxicityResponse(isToxic, toxicWords, reason);
    }
    
    /**
     * Helper method to create a toxicity analysis response
     */
    private JSONObject createSimpleToxicityResponse(boolean isToxic, List<String> toxicWords, String reason) {
        JSONObject response = new JSONObject();
        response.put("isToxic", isToxic);
        response.put("toxicWords", toxicWords);
        response.put("reason", reason);
        return response;
    }
} 