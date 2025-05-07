package com.esprit.services;

import com.esprit.models.enums.GoalTypeEnum;
import com.esprit.models.Saison;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AIContentGeneratorService {

    private static final String GEMINI_API_KEY = "AIzaSyAsl7GkBjs2SyMHIJueuSS3KKVnaEziWxw"; // Get free key from Google AI Studio
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + GEMINI_API_KEY;
    private final HttpClient httpClient;
    private final Gson gson;

    public AIContentGeneratorService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public static class GeneratedContent {
        public String title;
        public String description;

        public GeneratedContent(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    public GeneratedContent generateMissionContent(GoalTypeEnum goalType, int goalValue, int points, Saison saison) {
        try {
            String prompt = buildPrompt(goalType, goalValue, points, saison);
            String response = callGemini(prompt);
            return parseGeminiResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            // Return fallback content if AI fails
            return generateLocalContent(goalType, goalValue, points, saison);
        }
    }

    private String buildPrompt(GoalTypeEnum goalType, int goalValue, int points, Saison saison) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate an engaging mission title and description for a university club competition with these parameters:\n");
        prompt.append("- Goal: ").append(getGoalDescription(goalType, goalValue)).append("\n");
        prompt.append("- Reward: ").append(points).append(" points\n");

        if (saison != null) {
            prompt.append("- Season: ").append(saison.getNomSaison()).append("\n");
        }

        prompt.append("\nCriteria:\n");
        prompt.append("1. The title should be short (3-6 words), engaging, and motivational\n");
        prompt.append("2. The description should be 1-2 sentences, explaining the goal clearly\n");
        prompt.append("3. Make it appropriate for university students and club activities\n");
        prompt.append("4. Use encouraging language that motivates participation\n");
        prompt.append("\nFormat the response as JSON with 'title' and 'description' fields.");

        return prompt.toString();
    }

    private String getGoalDescription(GoalTypeEnum goalType, int goalValue) {
        switch (goalType) {
            case EVENT_COUNT:
                return "Create " + goalValue + " events";
            case EVENT_LIKES:
                return "Get " + goalValue + " likes on events";
            case MEMBER_COUNT:
                return "Recruit " + goalValue + " new members";
            default:
                return goalValue + " " + goalType.toString();
        }
    }

    private String callGemini(String prompt) throws Exception {
        JsonObject requestBody = new JsonObject();
        JsonObject content = new JsonObject();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);

        com.google.gson.JsonArray parts = new com.google.gson.JsonArray();
        parts.add(part);

        content.add("parts", parts);

        com.google.gson.JsonArray contents = new com.google.gson.JsonArray();
        contents.add(content);

        requestBody.add("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error: " + response.body());
        }

        return response.body();
    }

    private GeneratedContent parseGeminiResponse(String jsonResponse) {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String content = response.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            // Try to parse as JSON first
            try {
                JsonObject contentJson = JsonParser.parseString(content).getAsJsonObject();
                return new GeneratedContent(
                        contentJson.get("title").getAsString(),
                        contentJson.get("description").getAsString()
                );
            } catch (Exception e) {
                // If not JSON, parse as text
                String[] lines = content.split("\n", 2);
                return new GeneratedContent(
                        lines[0].replaceAll("^Title:\\s*", "").trim(),
                        lines.length > 1 ? lines[1].replaceAll("^Description:\\s*", "").trim() : ""
                );
            }
        } catch (Exception e) {
            return generateLocalContent(null, 0, 0, null);
        }
    }

    // Fallback local generation
    private GeneratedContent generateLocalContent(GoalTypeEnum goalType, int goalValue, int points, Saison saison) {
        String title;
        String description;

        // Generate title based on goal type
        switch (goalType) {
            case EVENT_COUNT:
                title = "Event Creation Challenge";
                description = String.format("Create %d amazing events and earn %d points! Show your creativity and organizational skills by planning engaging activities for the club community.", goalValue, points);
                break;
            case EVENT_LIKES:
                title = "Social Media Star";
                description = String.format("Gather %d likes on your events to earn %d points! Make your events popular and engage with the community through social media.", goalValue, points);
                break;
            case MEMBER_COUNT:
                title = "Recruitment Drive";
                description = String.format("Recruit %d new members to earn %d points! Grow our community by bringing in enthusiastic new members to join our activities.", goalValue, points);
                break;
            default:
                title = "Club Mission";
                description = String.format("Complete this mission by achieving %d %s to earn %d points!", goalValue, goalType.toString().toLowerCase(), points);
        }

        // Add season context if available
        if (saison != null) {
            description += " This mission is part of the " + saison.getNomSaison() + ".";
        }

        return new GeneratedContent(title, description);
    }
}