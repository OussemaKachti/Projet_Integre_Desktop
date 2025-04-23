package com.esprit.utils;

import com.esprit.models.Commentaire;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for analyzing comments using OpenAI GPT-4o mini
 * to generate insights, themes and visualizations
 */
public class CommentInsightsService {
    private static final Logger LOGGER = Logger.getLogger(CommentInsightsService.class.getName());
    private static CommentInsightsService instance;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";

    private String apiKey;

    private CommentInsightsService() {
        // Get API key from environment variables or configuration
        apiKey = System.getenv("OPENAI_API_KEY");

        // If not found in environment variables, try from ConfigurationManager
        if (apiKey == null || apiKey.isEmpty()) {
            try {
                ConfigurationManager config = ConfigurationManager.getInstance();
                apiKey = config.getProperty("OPENAI_API_KEY");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load configuration manager", e);
            }
        }

        // If still not found, log a warning
        if (apiKey == null || apiKey.isEmpty()) {
            LOGGER.warning("OpenAI API key not found. Comment insights will not work.");
        }
    }

    public static synchronized CommentInsightsService getInstance() {
        if (instance == null) {
            instance = new CommentInsightsService();
        }
        return instance;
    }

    /**
     * Generate insights from a list of comments
     * 
     * @param comments List of comments to analyze
     * @return Map containing insights data
     */
    public Map<String, Object> generateInsights(List<Commentaire> comments) {
        if (comments == null || comments.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("error", "No comments to analyze");
            return emptyResult;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "API key not configured");
            return errorResult;
        }

        // Extract comment text
        List<String> commentTexts = comments.stream()
                .map(Commentaire::getContenuComment)
                .filter(text -> text != null && !text.isEmpty())
                .collect(Collectors.toList());

        if (commentTexts.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("error", "No valid comment text to analyze");
            return emptyResult;
        }

        try {
            // Create the OpenAI request
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL);
            requestBody.put("temperature", 0.3);
            requestBody.put("response_format", new JSONObject().put("type", "json_object"));

            JSONArray messages = new JSONArray();

            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content",
                    "You are a comment analysis expert. Analyze the provided comments and generate insights. " +
                            "Respond with a JSON object containing:\n" +
                            "- summary (string): A concise 2-3 sentence summary of all comments\n" +
                            "- overallSentiment (string): either 'positive', 'negative', or 'neutral'\n" +
                            "- sentimentBreakdown (object): with keys 'positive', 'negative', 'neutral' and numeric values (0-1)\n"
                            +
                            "- mainThemes (array): 3-5 main themes found in comments, each an object with:\n" +
                            "  * name (string): the theme name\n" +
                            "  * count (number): frequency/relevance score (1-10)\n" +
                            "  * sentiment (string): theme sentiment ('positive', 'negative', or 'neutral')\n" +
                            "  * keywords (array): related keywords/phrases\n" +
                            "- keyFindings (array): 3-5 key insights/findings as strings\n" +
                            "- userFeedback (array): 2-3 most actionable pieces of feedback from users\n" +
                            "DO NOT include any explanation or text outside the JSON structure.");
            messages.put(systemMessage);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", "Analyze these comments from our platform and generate insights:\n\n" +
                    String.join("\n---\n", commentTexts));
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

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                LOGGER.warning("Failed to analyze comments, API returned code: " + responseCode);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "API error: " + responseCode);
                return errorResult;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                String content = message.getString("content");

                // Parse the content as JSON
                JSONObject insightsData = new JSONObject(content);
                return jsonToMap(insightsData);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during comment analysis", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Error analyzing comments: " + e.getMessage());
            return errorResult;
        }

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("error", "Unknown error analyzing comments");
        return errorResult;
    }

    /**
     * Convert a JSONObject to a Map, with special handling for nested arrays and
     * objects
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> result = new HashMap<>();

        for (String key : json.keySet()) {
            Object value = json.get(key);

            if (value instanceof JSONObject) {
                result.put(key, jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                result.put(key, jsonArrayToList((JSONArray) value));
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * Convert a JSONArray to a List, with special handling for nested arrays and
     * objects
     */
    @SuppressWarnings("unchecked")
    private List<Object> jsonArrayToList(JSONArray array) {
        List<Object> result = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);

            if (value instanceof JSONObject) {
                result.add(jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                result.add(jsonArrayToList((JSONArray) value));
            } else {
                result.add(value);
            }
        }

        return result;
    }
}