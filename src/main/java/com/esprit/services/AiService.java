package com.esprit.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AiService {
    private final String API_KEY = "your-openai-api-key";
    private final HttpClient client = HttpClient.newHttpClient();

    public boolean isToxic(String text) {
        // Implémentation simplifiée - à adapter selon vos besoins
        String[] toxicWords = { "bad", "hate", "stupid", "ugly" };
        text = text.toLowerCase();

        for (String word : toxicWords) {
            if (text.contains(word))
                return true;
        }
        return false;
    }

    public Map<String, Object> analyzeToxicity(String text) {
        Map<String, Object> result = new HashMap<>();
        result.put("isToxic", isToxic(text));
        result.put("toxicWords", findToxicWords(text));
        result.put("reason", "Inappropriate language detected");
        return result;
    }

    private String[] findToxicWords(String text) {
        // Implémentation à adapter selon vos besoins
        return new String[] { "bad", "hate" };
    }
}