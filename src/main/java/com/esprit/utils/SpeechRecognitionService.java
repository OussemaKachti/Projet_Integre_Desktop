package com.esprit.utils;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpeechRecognitionService {
    private static final Logger LOGGER = Logger.getLogger(SpeechRecognitionService.class.getName());
    private static SpeechRecognitionService instance;
    private Object recognizer; // Using Object instead of LiveSpeechRecognizer to avoid direct reference
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isRecognizing = new AtomicBoolean(false);
    private TextArea targetTextArea;
    private Button micButton;
    private boolean isAvailable = false;

    private SpeechRecognitionService() {
        // Try to load the Sphinx libraries
        try {
            // Try to dynamically load Sphinx classes to see if they're available
            Class<?> configClass = Class.forName("edu.cmu.sphinx.api.Configuration");
            Class<?> recognizerClass = Class.forName("edu.cmu.sphinx.api.LiveSpeechRecognizer");
            
            // If we get here, the classes are available - initialize the speech recognizer
            isAvailable = true;
            initializeRecognizer();
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Speech recognition libraries not found. Voice input will be disabled.", e);
            isAvailable = false;
            
            // Show alert message on JavaFX thread
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Speech Recognition Unavailable");
                alert.setHeaderText("Speech Recognition Libraries Not Found");
                alert.setContentText("The CMU Sphinx speech recognition libraries could not be loaded. " +
                        "Voice input functionality will be disabled. Please check your project dependencies.");
                alert.show();
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize speech recognizer", e);
            isAvailable = false;
        }
    }
    
    private void initializeRecognizer() {
        try {
            // Use reflection to avoid direct class references that would cause ClassNotFoundError
            Class<?> configClass = Class.forName("edu.cmu.sphinx.api.Configuration");
            Object configuration = configClass.getDeclaredConstructor().newInstance();
            
            // Set path to the acoustic model
            configClass.getMethod("setAcousticModelPath", String.class)
                     .invoke(configuration, "resource:/edu/cmu/sphinx/models/en-us/en-us");
            
            // Set path to the language model (vocabulary and grammar)
            configClass.getMethod("setDictionaryPath", String.class)
                     .invoke(configuration, "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
            
            configClass.getMethod("setLanguageModelPath", String.class)
                     .invoke(configuration, "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
            
            // Initialize the speech recognizer
            Class<?> recognizerClass = Class.forName("edu.cmu.sphinx.api.LiveSpeechRecognizer");
            recognizer = recognizerClass.getDeclaredConstructor(configClass)
                                      .newInstance(configuration);
                                      
            isAvailable = true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize speech recognizer", e);
            isAvailable = false;
        }
    }

    public static synchronized SpeechRecognitionService getInstance() {
        if (instance == null) {
            instance = new SpeechRecognitionService();
        }
        return instance;
    }

    public void startRecognition(TextArea textArea, Button button) {
        if (!isAvailable) {
            LOGGER.warning("Speech recognition is not available");
            showSpeechUnavailableMessage(button);
            return;
        }
        
        if (recognizer == null) {
            LOGGER.warning("Speech recognizer is not initialized");
            return;
        }
        
        // If already recognizing, stop it
        if (isRecognizing.get()) {
            stopRecognition();
            return;
        }
        
        this.targetTextArea = textArea;
        this.micButton = button;
        
        // Start recognition in a separate thread
        executor.submit(() -> {
            try {
                isRecognizing.set(true);
                
                // Update UI to show recording state
                Platform.runLater(() -> {
                    if (micButton != null) {
                        micButton.getStyleClass().add("recording");
                    }
                });
                
                // Use reflection to call methods on the recognizer object
                Class<?> recognizerClass = recognizer.getClass();
                recognizerClass.getMethod("startRecognition", boolean.class)
                              .invoke(recognizer, true);
                
                StringBuilder textResult = new StringBuilder();
                if (targetTextArea != null && !targetTextArea.getText().isEmpty()) {
                    textResult.append(targetTextArea.getText()).append(" ");
                }
                
                while (isRecognizing.get()) {
                    // Get result using reflection
                    Object result = recognizerClass.getMethod("getResult").invoke(recognizer);
                    
                    if (result != null) {
                        // Get hypothesis string from result
                        String hypothesis = (String) result.getClass()
                                                        .getMethod("getHypothesis")
                                                        .invoke(result);
                        
                        if (hypothesis != null && !hypothesis.trim().isEmpty()) {
                            textResult.append(hypothesis).append(" ");
                            String finalText = textResult.toString();
                            
                            // Update UI on the JavaFX thread
                            Platform.runLater(() -> {
                                if (targetTextArea != null) {
                                    targetTextArea.setText(finalText);
                                }
                            });
                        }
                    }
                }
                
                // Stop recognition using reflection
                recognizerClass.getMethod("stopRecognition").invoke(recognizer);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during speech recognition", e);
            } finally {
                // Ensure we reset UI even if an error occurs
                Platform.runLater(() -> {
                    if (micButton != null) {
                        micButton.getStyleClass().remove("recording");
                    }
                });
                isRecognizing.set(false);
            }
        });
    }

    private void showSpeechUnavailableMessage(Button button) {
        Platform.runLater(() -> {
            // Update the button appearance to indicate it's disabled
            if (button != null) {
                button.setDisable(true);
                
                // Show a tooltip or small alert to inform the user
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Feature Unavailable");
                alert.setHeaderText("Speech Recognition Unavailable");
                alert.setContentText("Voice input is currently unavailable. Please check if the required libraries are installed.");
                alert.show();
            }
        });
    }

    public void stopRecognition() {
        isRecognizing.set(false);
        
        // Update UI to show stopped state
        Platform.runLater(() -> {
            if (micButton != null) {
                micButton.getStyleClass().remove("recording");
            }
        });
    }

    public boolean isRecognizing() {
        return isRecognizing.get();
    }
    
    public boolean isAvailable() {
        return isAvailable;
    }

    public void shutdown() {
        stopRecognition();
        executor.shutdown();
    }
} 