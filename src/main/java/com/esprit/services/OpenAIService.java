package com.esprit.services;

import com.esprit.models.Commentaire;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
<<<<<<< HEAD
=======
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
<<<<<<< HEAD
=======
import java.util.Properties;
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
import org.json.JSONArray;
import org.json.JSONObject;

public class OpenAIService {
<<<<<<< HEAD
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    
    public OpenAIService() {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("OpenAI API key not found in environment variables");
=======
    private String apiKey;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    
    public OpenAIService() {
        // Try to get the key from environment variable
        apiKey = System.getenv("OPENAI_API_KEY");
        
        // If not found, try to get it from properties file
        if (apiKey == null || apiKey.isEmpty()) {
            try {
                // Try to load from config.properties in user's home directory
                File configFile = new File(System.getProperty("user.home"), "config.properties");
                if (configFile.exists()) {
                    Properties props = new Properties();
                    try (FileInputStream in = new FileInputStream(configFile)) {
                        props.load(in);
                        apiKey = props.getProperty("OPENAI_API_KEY");
                    }
                }
                
                // If still not found, try to load from classpath
                if (apiKey == null || apiKey.isEmpty()) {
                    try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                        if (in != null) {
                            Properties props = new Properties();
                            props.load(in);
                            apiKey = props.getProperty("OPENAI_API_KEY");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading API key from config file: " + e.getMessage());
            }
        }
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API key not found in environment variables or config.properties file");
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
        }
    }

    public String summarizeComments(List<Commentaire> comments) {
        if (comments == null || comments.isEmpty()) {
            return "No comments to summarize.";
        }

        // Prepare the comments for the prompt
<<<<<<< HEAD
        StringBuilder commentsText = new StringBuilder();
        for (Commentaire comment : comments) {
            commentsText.append("- ").append(comment.getContenuComment()).append("\n");
        }

        // Create the prompt
        String prompt = String.format(
            "Please provide a concise summary of the following comments, highlighting the main points and sentiment:\n\n%s",
            commentsText.toString()
        );
=======
        List<String> commentTexts = new ArrayList<>();
        for (Commentaire comment : comments) {
            commentTexts.add(comment.getContenuComment());
        }

        // Create a better prompt for summarization
        String prompt = "Summarize these comments in 2 to 3 sentences in a clear and impactful way.\n" +
            "- Identify the main topic.\n" +
            "- Capture general opinions (positive, negative, mixed).\n" +
            "- Provide a smooth and readable summary, as if writing a professional article.\n\n" +
            "Comments to analyze:\n\n" + String.join("\n", commentTexts);
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87

        try {
            // Create the request body
            JSONObject requestBody = new JSONObject();
<<<<<<< HEAD
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);
=======
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("temperature", 0.5);
            requestBody.put("max_tokens", 150);
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
            
            JSONArray messages = new JSONArray();
            
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
<<<<<<< HEAD
            systemMessage.put("content", "You are a helpful assistant that summarizes comments.");
=======
            systemMessage.put("content", "You are an AI that summarizes user comments.");
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
            messages.put(systemMessage);
            
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);
            
            requestBody.put("messages", messages);
            
            // Make the API request
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
<<<<<<< HEAD
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
=======
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
            conn.setDoOutput(true);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
<<<<<<< HEAD
            // Read the response
=======
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
                return "Error: " + errorMessage;
            }
            
            // If we get here, read the successful response
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
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
                return message.getString("content");
            }
            
            return "Failed to generate summary.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating summary: " + e.getMessage();
        }
<<<<<<< HEAD
    }}
=======
    }
}
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
