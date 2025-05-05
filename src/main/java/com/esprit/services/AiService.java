package com.esprit.services;

public class AiService {
    public boolean isToxic(String text) {
        // Simple implementation - you can enhance this with actual AI/ML logic
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Check for common inappropriate words or patterns
        String[] toxicWords = {
            "badword1", "badword2", "badword3",
            // Add more toxic words as needed
        };
        
        text = text.toLowerCase();
        for (String word : toxicWords) {
            if (text.contains(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isAppropriate(String text) {
        return !isToxic(text);
    }
    
    public String filterInappropriateContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        // Replace inappropriate words with asterisks
        String[] inappropriateWords = {
            "badword1", "badword2", "badword3"
            // Add more inappropriate words as needed
        };
        
        String filteredText = text;
        for (String word : inappropriateWords) {
            String asterisks = "*".repeat(word.length());
            filteredText = filteredText.replaceAll("(?i)" + word, asterisks);
        }
        
        return filteredText;
    }
} 