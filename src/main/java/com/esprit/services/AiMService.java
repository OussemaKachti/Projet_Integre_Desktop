package com.esprit.services;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class AiMService {

    private static final String API_KEY = "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";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";



    public static boolean containsBadWords(String text) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = """
        {
          "model": "mistralai/mistral-7b-instruct",
          "messages": [
            {"role": "system", "content": "You are a content moderator. Only reply YES if the text contains insults, bad words, or inappropriate language. Reply NO if it's clean."},
            {"role": "user", "content": "%s"}
          ],
          "temperature": 0.0
        }
        """.formatted(text);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream(), "utf-8")) {
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
        }

        conn.disconnect();

        // Very simple check: we just look if "YES" appears in the AI response
        return response.toString().toLowerCase().contains("yes");
    }
}
