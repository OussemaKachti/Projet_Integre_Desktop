package com.esprit.utils;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import javafx.scene.control.Label;
import javafx.application.Platform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TranslationService {
    private static TranslationService instance;
    private final OpenAiService openAiService;
    private final ExecutorService executorService;
    
    private TranslationService() {
        String apiKey = getApiKeyFromProperties();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API key not found in config.properties");
        }
        this.openAiService = new OpenAiService(apiKey);
        this.executorService = Executors.newCachedThreadPool();
    }
    
    private String getApiKeyFromProperties() {
        Properties properties = new Properties();
        
        // Try loading from classpath first
        try {
            properties.load(getClass().getResourceAsStream("/config.properties"));
            String key = properties.getProperty("OPENAI_API_KEY");
            if (key != null && !key.isEmpty()) {
                return key;
            }
        } catch (Exception e) {
            // Ignore and try next location
        }
        
        // Try loading from current directory
        try {
            properties.load(new java.io.FileInputStream("config.properties"));
            String key = properties.getProperty("OPENAI_API_KEY");
            if (key != null && !key.isEmpty()) {
                return key;
            }
        } catch (Exception e) {
            // Ignore and try next location
        }
        
        // Try loading from user home directory
        try {
            String userHome = System.getProperty("user.home");
            properties.load(new java.io.FileInputStream(userHome + "/config.properties"));
            String key = properties.getProperty("OPENAI_API_KEY");
            if (key != null && !key.isEmpty()) {
                return key;
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return null;
    }
    
    public static TranslationService getInstance() {
        if (instance == null) {
            instance = new TranslationService();
        }
        return instance;
    }
    
    public void detectAndTranslate(String text, Label translatedContent, Label originalLanguageLabel, Label translatedLanguageLabel) {
        if (openAiService == null) {
            Platform.runLater(() -> {
                translatedContent.setText("Error: OpenAI service is not configured. Please check your API key configuration.");
                originalLanguageLabel.setText("");
                translatedLanguageLabel.setText("");
            });
            return;
        }

        // Show loading state
        Platform.runLater(() -> {
            translatedContent.setText("Translating...");
            originalLanguageLabel.setText("Detecting language...");
            translatedLanguageLabel.setText("");
        });
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // First, detect the language
                List<ChatMessage> detectMessages = new ArrayList<>();
                detectMessages.add(new ChatMessage("system", 
                    "You are a language detection expert. Respond only with the language name in English."));
                detectMessages.add(new ChatMessage("user", 
                    "What language is this text written in: \"" + text + "\""));
                
                ChatCompletionRequest detectRequest = ChatCompletionRequest.builder()
                    .messages(detectMessages)
                    .model("gpt-4o-mini")
                    .build();
                
                String detectedLanguage = openAiService.createChatCompletion(detectRequest)
                    .getChoices().get(0).getMessage().getContent();
                
                // Determine target language
                String targetLanguage = detectedLanguage.toLowerCase().contains("english") ? "French" : "English";
                
                // Then, translate the text
                List<ChatMessage> translateMessages = new ArrayList<>();
                translateMessages.add(new ChatMessage("system", 
                    "You are a professional translator. Translate the text to " + targetLanguage + 
                    ". Respond only with the translation, no additional text."));
                translateMessages.add(new ChatMessage("user", text));
                
                ChatCompletionRequest translateRequest = ChatCompletionRequest.builder()
                    .messages(translateMessages)
                    .model("gpt-4o-mini")
                    .build();
                
                String translatedText = openAiService.createChatCompletion(translateRequest)
                    .getChoices().get(0).getMessage().getContent();
                
                // Update UI with results
                Platform.runLater(() -> {
                    translatedContent.setText(translatedText);
                    originalLanguageLabel.setText("Original (" + detectedLanguage + ")");
                    translatedLanguageLabel.setText("Translation (" + targetLanguage + ")");
                });
                
                return true;
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Unknown error occurred";
                }
                
                // Check for common API errors
                if (errorMessage.contains("401")) {
                    errorMessage = "Invalid API key. Please check your OpenAI API key configuration.";
                } else if (errorMessage.contains("429")) {
                    errorMessage = "Rate limit exceeded. Please try again later.";
                } else if (errorMessage.contains("500")) {
                    errorMessage = "OpenAI service error. Please try again later.";
                }
                
                final String finalError = errorMessage;
                Platform.runLater(() -> {
                    translatedContent.setText("Translation failed: " + finalError);
                    originalLanguageLabel.setText("");
                    translatedLanguageLabel.setText("");
                });
                return false;
            }
        }, executorService);
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
} 